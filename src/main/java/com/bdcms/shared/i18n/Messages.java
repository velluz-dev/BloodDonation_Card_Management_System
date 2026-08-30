package com.bdcms.shared.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Interface text in the operator's language.
 *
 * <p>Every string a user reads lives in {@code messages.properties} (English)
 * or {@code messages_my.properties} (Burmese) rather than in a JSP or a servlet.
 * That is not tidiness for its own sake: a term like "not eligible" appears on
 * six screens, and a translation that reads two different ways on two of them
 * is how staff start to distrust the wording. One key, one translation.
 *
 * <p>JSPs reach the same bundles through JSTL's {@code <fmt:message>}; this
 * class is for the strings servlets produce - flash messages and validation
 * errors - which never pass through a JSP that could translate them.
 *
 * <h2>Missing keys fall back rather than shout</h2>
 *
 * <p>A key absent from the Burmese bundle resolves to the English one, because
 * {@link ResourceBundle} walks the parent chain. A half-finished translation
 * therefore shows English in the gaps, not {@code ???key???}. That matters
 * while the Burmese is still being reviewed: a missing string should look
 * unfinished, not broken.
 */
public final class Messages {

    /** Base name of the bundle pair, resolved from the classpath. */
    public static final String BUNDLE = "messages";

    private Messages() {
    }

    /**
     * The translation for {@code key}, or the key itself if it exists in
     * neither bundle.
     *
     * <p>Returning the key rather than throwing is deliberate: a typo in a key
     * should leave one odd-looking label on a page, not a 500 that takes the
     * whole screen down.
     */
    public static String get(Locale locale, String key) {
        try {
            return ResourceBundle.getBundle(BUNDLE, Locales.orDefault(locale)).getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * As {@link #get}, with {@code {0}}, {@code {1}} … substituted.
     *
     * <p>Placeholders rather than string concatenation, because word order
     * differs between the two languages: "{0} may not donate today" and its
     * Burmese equivalent put the name in different places, and only a
     * placeholder lets each bundle decide where.
     */
    public static String format(Locale locale, String key, Object... args) {
        String pattern = get(locale, key);
        if (args == null || args.length == 0) {
            return pattern;
        }
        // MessageFormat is locale-sensitive for embedded numbers and dates.
        return new MessageFormat(pattern, Locales.orDefault(locale)).format(args);
    }
}
