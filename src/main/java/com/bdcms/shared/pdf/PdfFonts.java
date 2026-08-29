package com.bdcms.shared.pdf;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.BaseFont;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;

/**
 * Fonts for generated PDFs.
 *
 * <p>The built-in Helvetica and Courier that PDF gives away for free use
 * WinAnsi, a single-byte Latin encoding. A donor named in Burmese therefore
 * cannot be represented in them at all - the name silently vanishes from the
 * printed card. This class fixes that by embedding a Unicode font and choosing
 * between the two per string.
 *
 * <p><strong>Padauk</strong> (SIL Open Font License) covers Latin and the
 * Myanmar block, and is embedded with {@code IDENTITY_H}, the two-byte encoding
 * that lets a PDF address glyphs beyond Latin-1.
 *
 * <h2>The limit worth knowing about</h2>
 *
 * <p>OpenPDF has no complex-text shaping engine. Burmese needs one: several
 * signs are stored after the consonant they attach to but drawn before it, and
 * a shaper is what performs that reordering. Embedding the font makes the
 * glyphs <em>present and selectable</em> rather than missing, which is a real
 * improvement over a blank; it does not guarantee that every multi-part
 * syllable stacks the way a native reader expects.
 *
 * <p>Latin text is unaffected either way - it keeps using Helvetica, so the
 * cards and reports look exactly as designed for the common case.
 */
public final class PdfFonts {

    private static final String PADAUK = "/fonts/Padauk-Regular.ttf";

    /**
     * Loaded once. Null if the font is missing from the WAR, in which case
     * every call falls back to Helvetica - a card that prints a name badly is
     * better than a card that fails to print.
     */
    private static final BaseFont UNICODE = loadPadauk();

    private PdfFonts() {
    }

    /**
     * True when {@code text} contains a character from the Myanmar block
     * (U+1000-U+109F) or its extensions, and therefore cannot be written in
     * WinAnsi.
     */
    public static boolean needsUnicode(String text) {
        if (text == null) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            // Myanmar, Myanmar Extended-A, Myanmar Extended-B.
            if ((c >= 0x1000 && c <= 0x109F)
                    || (c >= 0xAA60 && c <= 0xAA7F)
                    || (c >= 0xA9E0 && c <= 0xA9FF)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A font able to render {@code text}.
     *
     * <p>Helvetica for Latin, the embedded Unicode face when the string needs
     * it. Chosen per string rather than applied wholesale so that the ordinary
     * card keeps the typography it was designed with, and only the field that
     * actually carries Burmese pays for the embedded font.
     *
     * @param base one of the {@link FontFactory} names, e.g. {@code HELVETICA}
     */
    public static Font forText(String text, String base, float size, int style, Color colour) {
        if (needsUnicode(text) && UNICODE != null) {
            return new Font(UNICODE, size, style, colour);
        }
        return FontFactory.getFont(base, size, style, colour);
    }

    /** Convenience for body text. */
    public static Font forText(String text, float size, int style, Color colour) {
        return forText(text, FontFactory.HELVETICA, size, style, colour);
    }

    /** True when the Unicode face is available in this build. */
    public static boolean unicodeAvailable() {
        return UNICODE != null;
    }

    private static BaseFont loadPadauk() {
        try (InputStream in = PdfFonts.class.getResourceAsStream(PADAUK)) {
            if (in == null) {
                System.getLogger(PdfFonts.class.getName()).log(System.Logger.Level.WARNING,
                        "Padauk not found on the classpath at " + PADAUK
                                + " - PDFs will fall back to Helvetica and cannot render Burmese.");
                return null;
            }
            byte[] bytes = in.readAllBytes();
            // The name must keep its .ttf suffix: OpenPDF decides how to parse
            // the bytes from it, not from the content.
            return BaseFont.createFont("Padauk-Regular.ttf", BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED, BaseFont.CACHED, bytes, null);
        } catch (IOException | RuntimeException e) {
            System.getLogger(PdfFonts.class.getName()).log(System.Logger.Level.WARNING,
                    "Could not load the Unicode PDF font; falling back to Helvetica.", e);
            return null;
        }
    }
}
