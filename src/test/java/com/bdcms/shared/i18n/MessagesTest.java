package com.bdcms.shared.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the two failure modes a translated interface actually suffers from.
 *
 * <p>Neither is a logic bug, which is why neither shows up in any other test:
 * one is a file-encoding accident that turns Burmese into mojibake, and the
 * other is a new English string that nobody remembered to translate. Both look
 * fine to the compiler and are found by a user.
 */
class MessagesTest {

    @Test
    @DisplayName("English bundle resolves")
    void englishResolves() {
        assertEquals("Donors", Messages.get(Locales.ENGLISH, "common.donors"));
    }

    @Test
    @DisplayName("Burmese bundle resolves, and survives the round trip through the file")
    void burmeseResolves() {
        String donors = Messages.get(Locales.MYANMAR, "common.donors");

        // The specific risk: .properties files were ISO-8859-1 before Java 9,
        // and a build that reverts to that reads every Burmese byte as a Latin
        // character. The text still renders - as Ã¡Ã€ nonsense - so only a
        // codepoint check catches it.
        assertNotEquals("Donors", donors, "Burmese bundle did not load at all");
        assertTrue(donors.codePoints().anyMatch(c -> c >= 0x1000 && c <= 0x109F),
                "no Myanmar-block characters in '" + donors + "' - the bundle was "
                        + "probably read as ISO-8859-1 rather than UTF-8");
    }

    @Test
    @DisplayName("A key missing from Burmese falls back to English, not to ???key???")
    void missingKeyFallsBackToEnglish() {
        // messages_my.properties deliberately leaves this one English.
        assertEquals(Messages.get(Locales.ENGLISH, "common.appName"),
                Messages.get(Locales.MYANMAR, "common.appName"));
    }

    @Test
    @DisplayName("An unknown key returns itself rather than throwing")
    void unknownKeyReturnsKey() {
        // A typo should cost one odd label, not the whole screen.
        assertEquals("no.such.key", Messages.get(Locales.ENGLISH, "no.such.key"));
    }

    @Test
    @DisplayName("Placeholders are substituted, and each language may position them differently")
    void placeholdersSubstitute() {
        String en = Messages.format(Locales.ENGLISH, "verify.mayNotDonate", "Aung Aung");
        String my = Messages.format(Locales.MYANMAR, "verify.mayNotDonate", "Aung Aung");

        assertAll(
                () -> assertTrue(en.contains("Aung Aung")),
                () -> assertTrue(my.contains("Aung Aung")),
                () -> assertFalse(en.contains("{0}"), "placeholder left unsubstituted"),
                () -> assertFalse(my.contains("{0}"), "placeholder left unsubstituted"));
    }

    @Test
    @DisplayName("Every English key is translated into Burmese")
    void burmeseCoversEveryKey() throws IOException {
        // The point of failing the build here: a screen that is 90% Burmese and
        // 10% English reads as broken rather than as untranslated, and the gap
        // is invisible to anyone who only tests in English.
        // Read both files directly rather than through getBundle: the Burmese
        // bundle inherits the English one as its parent, so asking it for its
        // keys would report full coverage however little of it is translated.
        Properties en = load("/messages.properties");
        Properties my = load("/messages_my.properties");

        var untranslated = new TreeSet<>(en.stringPropertyNames());
        untranslated.removeAll(my.stringPropertyNames());

        // messages_my.properties keeps these in English on purpose: a product
        // name, and the label of a language written in its own language.
        untranslated.removeAll(List.of("common.appName", "lang.english"));

        assertTrue(untranslated.isEmpty(),
                untranslated.size() + " key(s) have no Burmese translation: " + untranslated);
    }

    @Test
    @DisplayName("Only the two supported languages are accepted")
    void unsupportedLanguagesRejected() {
        assertAll(
                () -> assertEquals(Locales.MYANMAR, Locales.parse("my")),
                () -> assertEquals(Locales.MYANMAR, Locales.parse("  MY ")),
                () -> assertEquals(Locales.ENGLISH, Locales.parse("en")),
                () -> assertNull(Locales.parse("fr")),
                () -> assertNull(Locales.parse("")),
                () -> assertNull(Locales.parse(null)),
                // Not a real language tag: rejected before it can reach a
                // bundle lookup and probe the classpath.
                () -> assertNull(Locales.parse("../../etc/passwd")));
    }

    @Test
    @DisplayName("Apostrophes match whether the message takes parameters")
    void apostrophesAreEscapedConsistently() throws IOException {
        // MessageFormat treats ' as a quoting character, but JSTL only invokes
        // MessageFormat on a message that has <fmt:param> children. The two
        // rules therefore point in opposite directions, and getting it wrong is
        // invisible until a user reads the screen:
        //
        //   parameterised, single '  ->  the rest of the sentence disappears
        //   plain, doubled ''       ->  two apostrophes are printed
        //
        // Neither throws, so only this check catches them.
        var wrong = new TreeSet<String>();
        for (var bundle : List.of("/messages.properties", "/messages_my.properties")) {
            Properties p = load(bundle);
            for (String key : p.stringPropertyNames()) {
                String value = p.getProperty(key);
                boolean parameterised = value.contains("{0}") || value.contains("{1}");
                String stripped = value.replace("''", "");
                boolean hasSingle = stripped.contains("'");
                boolean hasDoubled = value.contains("''");

                if (parameterised && hasSingle) {
                    wrong.add(bundle + " " + key + " (parameterised, needs '')");
                } else if (!parameterised && hasDoubled) {
                    wrong.add(bundle + " " + key + " (no parameters, needs a single ')");
                }
            }
        }
        assertTrue(wrong.isEmpty(), "apostrophe escaping does not match arity: " + wrong);
    }

    /** Reads a bundle file as UTF-8, the encoding Java 9+ uses for them. */
    private static Properties load(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = MessagesTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "missing from the classpath: " + resource);
            properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
        return properties;
    }
}
