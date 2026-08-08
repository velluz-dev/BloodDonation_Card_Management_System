package com.bdcms.feature.report;

import com.bdcms.shared.config.AppConfig;
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

    public static byte[] export(Report report, String generatedBy) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Landscape: the donations and donor-activity reports are seven columns
        // wide and would wrap unreadably on portrait A4.
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 40, 36);

        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f, BLOOD_RED);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 9.5f, Color.DARK_GRAY);
        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 7.5f, Color.GRAY);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8.5f, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8.5f, Color.BLACK);

        document.add(paragraph(AppConfig.defaultCentre() + "  -  Blood Donation Card Management System",
                metaFont, 0));
        document.add(paragraph(report.title(), titleFont, 4));
        document.add(paragraph(report.subtitle(), subtitleFont, 2));
        document.add(paragraph("Generated " + GENERATED.format(LocalDateTime.now())
                + " by " + generatedBy, metaFont, 2));

        if (report.isEmpty()) {
            document.add(paragraph("No data for this report.", cellFont, 16));
            document.close();
            return out.toByteArray();
        }

        PdfPTable table = new PdfPTable(report.headers().size());
        table.setWidthPercentage(100);
        table.setSpacingBefore(14f);
        table.setHeaderRows(1);

        for (String header : report.headers()) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(BLOOD_RED);
            cell.setPadding(5f);
            cell.setBorderColor(BLOOD_RED);
            table.addCell(cell);
        }

        boolean shaded = false;
        for (ReportRow row : report.rows()) {
            for (int i = 0; i < report.headers().size(); i++) {
                PdfPCell cell = new PdfPCell(new Phrase(row.cell(i), cellFont));
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

        document.add(paragraph(report.getRowCount() + " row(s). "
                + "This report is generated from the centralised donor database and reflects "
                + "records at the time of generation.", metaFont, 10));

        document.close();
        return out.toByteArray();
    }

    private static Paragraph paragraph(String text, Font font, float spacingBefore) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setAlignment(Element.ALIGN_LEFT);
        return paragraph;
    }
}
