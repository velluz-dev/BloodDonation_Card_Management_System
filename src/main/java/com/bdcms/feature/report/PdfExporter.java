package com.bdcms.feature.report;

import com.bdcms.shared.config.AppConfig;
import com.bdcms.shared.i18n.Messages;
import com.bdcms.shared.pdf.PdfFonts;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Exports a {@link Report} to PDF (FR-7).
 *
 * <p>Takes the finished report rather than querying anything itself, so the PDF
 * is built from precisely the rows shown on screen - the export can never
 * disagree with the page it was exported from.
 */
public final class PdfExporter {

    private static final Color BLOOD_RED = new Color(0xA1, 0x1D, 0x21);
    private static final DateTimeFormatter GENERATED =
            DateTimeFormatter.ofPattern("dd MMM yyyy 'at' HH:mm");

    private PdfExporter() {
    }

    /**
     * Renders {@code report} to PDF in {@code locale}.
     *
     * <h2>Every heading picks its own font</h2>
     *
     * <p>The built-in Helvetica is WinAnsi and cannot encode a Myanmar
     * codepoint at all - the glyph does not fall back, it disappears. So no
     * string here may assume Latin: the title, the subtitle, the column
     * headings and the footer all go through
     * {@link PdfFonts#forText(String, String, float, int, Color)}, which keeps
     * Helvetica for Latin text and swaps in the embedded Unicode face only for
     * the strings that need it.
     *
     * <p>An English report is therefore byte-for-byte what it always was, and
     * a Burmese one embeds a font subset instead of printing gaps.
     */
    public static byte[] export(Report report, String generatedBy, Locale locale) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Landscape: the donations and donor-activity reports are seven columns
        // wide and would wrap unreadably on portrait A4.
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 40, 36);

        PdfWriter.getInstance(document, out);
        document.open();

        String banner = AppConfig.defaultCentre() + "  -  "
                + Messages.get(locale, "pdf.systemName");
        String generated = Messages.format(locale, "pdf.generated",
                GENERATED.format(LocalDateTime.now()), generatedBy);

        document.add(paragraph(banner, meta(banner), 0));
        document.add(paragraph(report.title(),
                PdfFonts.forText(report.title(), FontFactory.HELVETICA_BOLD, 16f,
                        Font.NORMAL, BLOOD_RED), 4));
        document.add(paragraph(report.subtitle(),
                PdfFonts.forText(report.subtitle(), FontFactory.HELVETICA, 9.5f,
                        Font.NORMAL, Color.DARK_GRAY), 2));
        document.add(paragraph(generated, meta(generated), 2));

        if (report.isEmpty()) {
            String none = Messages.get(locale, "report.noData");
            document.add(paragraph(none,
                    PdfFonts.forText(none, FontFactory.HELVETICA, 8.5f,
                            Font.NORMAL, Color.BLACK), 16));
            document.close();
            return out.toByteArray();
        }

        PdfPTable table = new PdfPTable(report.headers().size());
        table.setWidthPercentage(100);
        table.setSpacingBefore(14f);
        table.setHeaderRows(1);

        for (String header : report.headers()) {
            PdfPCell cell = new PdfPCell(new Phrase(header,
                    PdfFonts.forText(header, FontFactory.HELVETICA_BOLD, 8.5f,
                            Font.NORMAL, Color.WHITE)));
            cell.setBackgroundColor(BLOOD_RED);
            cell.setPadding(5f);
            cell.setBorderColor(BLOOD_RED);
            table.addCell(cell);
        }

        boolean shaded = false;
        for (ReportRow row : report.rows()) {
            for (int i = 0; i < report.headers().size(); i++) {
                // Any cell may hold a donor name, so each picks its own font.
                // Latin cells keep Helvetica; only the ones that need the
                // embedded Unicode face pay for it.
                String value = row.cell(i);
                PdfPCell cell = new PdfPCell(new Phrase(value,
                        PdfFonts.forText(value, FontFactory.HELVETICA, 8.5f,
                                Font.NORMAL, Color.BLACK)));
                cell.setPadding(4f);
                cell.setBorderColor(new Color(0xDD, 0xDD, 0xDD));
                if (shaded) {
                    cell.setBackgroundColor(new Color(0xF7, 0xF7, 0xF7));
                }
                table.addCell(cell);
            }
            shaded = !shaded;
        }
        document.add(table);

        String footer = Messages.format(locale, "pdf.footer", report.getRowCount());
        document.add(paragraph(footer, meta(footer), 10));

        document.close();
        return out.toByteArray();
    }

    /** The small grey face, in whichever script {@code text} needs. */
    private static Font meta(String text) {
        return PdfFonts.forText(text, FontFactory.HELVETICA, 7.5f, Font.NORMAL, Color.GRAY);
    }

    private static Paragraph paragraph(String text, Font font, float spacingBefore) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setAlignment(Element.ALIGN_LEFT);
        return paragraph;
    }
}
