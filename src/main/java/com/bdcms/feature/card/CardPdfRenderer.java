package com.bdcms.feature.card;

import com.bdcms.feature.donor.Donor;
import com.bdcms.feature.donor.PhotoStore;
import com.bdcms.shared.i18n.Messages;
import com.bdcms.shared.pdf.PdfFonts;
import com.bdcms.shared.config.AppConfig;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renders a printable donor card as a PDF (FR-3).
 *
 * <p>The page is sized to a standard ID-1 card (85.6 x 54 mm) rather than to A4,
 * so what comes out of the printer is the physical object the donor carries
 * rather than a picture of one on a sheet of paper.
 *
 * <p>Everything the proposal asks for is on the card: name, blood group,
 * photograph, card number and QR code. The national ID is deliberately absent -
 * a card that is lost in the street should not hand a finder the donor's
 * identity number.
 */
public final class CardPdfRenderer {

    private static final float MM = 72f / 25.4f;
    private static final Rectangle CARD_SIZE = new Rectangle(85.6f * MM, 54f * MM);
    private static final Color BLOOD_RED = new Color(0xA1, 0x1D, 0x21);
    private static final DateTimeFormatter ISSUED = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private CardPdfRenderer() {
    }

    /**
     * Renders the printable card in {@code locale}.
     *
     * <p>The donor's name has always chosen its own font, because a name can
     * carry any script. Now the printed labels can too - so each one asks
     * {@link PdfFonts} rather than assuming Helvetica, which is WinAnsi and
     * would drop a Myanmar glyph silently rather than substituting anything.
     */
    public static byte[] render(Donor donor, DonorCard card, Locale locale) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(CARD_SIZE, 8, 8, 8, 8);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8f, Color.BLACK);
            Font groupFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f, BLOOD_RED);
            Font monoFont = FontFactory.getFont(FontFactory.COURIER_BOLD, 8f, Color.BLACK);

            String title = Messages.get(locale, "pdfCard.title");
            String centre = AppConfig.defaultCentre();

            PdfPTable header = new PdfPTable(1);
            header.setWidthPercentage(100);
            header.addCell(borderless(new Phrase(title,
                    PdfFonts.forText(title, FontFactory.HELVETICA_BOLD, 8.5f,
                            Font.NORMAL, BLOOD_RED))));
            header.addCell(borderless(new Phrase(centre,
                    PdfFonts.forText(centre, FontFactory.HELVETICA, 6f,
                            Font.NORMAL, Color.DARK_GRAY))));
            document.add(header);

            PdfPTable body = new PdfPTable(new float[]{1.1f, 2.2f, 1.3f});
            body.setWidthPercentage(100);
            body.setSpacingBefore(4f);

            body.addCell(photoCell(donor, locale));
            body.addCell(detailsCell(donor, card, locale, valueFont, groupFont));
            body.addCell(qrCell(card, locale, monoFont));

            document.add(body);

            String issued = card.getIssuedOn() == null ? "-" : ISSUED.format(card.getIssuedOn());
            String note = Messages.format(locale, "pdfCard.footer", issued);
            PdfPCell footer = borderless(new Phrase(note,
                    PdfFonts.forText(note, FontFactory.HELVETICA, 5f,
                            Font.NORMAL, Color.GRAY)));
            PdfPTable footerTable = new PdfPTable(1);
            footerTable.setWidthPercentage(100);
            footerTable.setSpacingBefore(3f);
            footerTable.addCell(footer);
            document.add(footerTable);

            document.close();
            return out.toByteArray();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Could not render card PDF for " + card.getCardNo(), e);
        }
    }

    private static PdfPCell photoCell(Donor donor, Locale locale) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(0);
        try {
            Path file = PhotoStore.resolve(donor.getPhotoPath());
            if (file != null && Files.isRegularFile(file)) {
                Image photo = Image.getInstance(Files.readAllBytes(file));
                photo.scaleToFit(58f, 72f);
                cell.addElement(photo);
                return cell;
            }
        } catch (Exception ignored) {
            // Fall through to the textual placeholder below: a card must still
            // print even when the photograph is missing or unreadable.
        }
        String none = Messages.get(locale, "pdfCard.noPhoto");
        Phrase placeholder = new Phrase(none,
                PdfFonts.forText(none, FontFactory.HELVETICA, 6f, Font.NORMAL, Color.GRAY));
        PdfPCell fallback = new PdfPCell(placeholder);
        fallback.setBorder(Rectangle.BOX);
        fallback.setBorderColor(Color.LIGHT_GRAY);
        fallback.setFixedHeight(72f);
        fallback.setHorizontalAlignment(Element.ALIGN_CENTER);
        fallback.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return fallback;
    }

    private static PdfPCell detailsCell(Donor donor, DonorCard card, Locale locale,
                                        Font valueFont, Font groupFont) {
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);
        inner.addCell(borderless(label(locale, "pdfCard.donorName")));
        // The donor's name is the one field on the card that can carry a
        // non-Latin script, so it picks its own font. Everything else stays
        // Helvetica, and the card looks exactly as designed for Latin names.
        inner.addCell(borderless(new Phrase(donor.getFullName(),
                PdfFonts.forText(donor.getFullName(), FontFactory.HELVETICA_BOLD,
                        8f, Font.NORMAL, Color.BLACK))));
        inner.addCell(borderless(label(locale, "pdfCard.bloodGroup")));
        inner.addCell(borderless(new Phrase(
                donor.getBloodGroup() == null ? "-" : donor.getBloodGroup().getLabel(), groupFont)));
        inner.addCell(borderless(label(locale, "pdfCard.cardNumber")));
        inner.addCell(borderless(new Phrase(card.getCardNo(),
                FontFactory.getFont(FontFactory.COURIER_BOLD, 8f, Color.BLACK))));

        PdfPCell cell = new PdfPCell(inner);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingLeft(6f);
        return cell;
    }

    private static PdfPCell qrCell(DonorCard card, Locale locale, Font monoFont) {
        PdfPTable inner = new PdfPTable(1);
        inner.setWidthPercentage(100);
        try {
            Image qr = Image.getInstance(QrCodeGenerator.png(card.getCardNo(), 300));
            qr.scaleToFit(62f, 62f);
            PdfPCell qrImage = new PdfPCell(qr, false);
            qrImage.setBorder(Rectangle.NO_BORDER);
            qrImage.setHorizontalAlignment(Element.ALIGN_CENTER);
            inner.addCell(qrImage);
        } catch (Exception e) {
            inner.addCell(borderless(label(locale, "pdfCard.qrUnavailable")));
        }
        inner.addCell(borderless(label(locale, "pdfCard.scanAtCounter")));

        PdfPCell cell = new PdfPCell(inner);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }

    /** A small grey caption, in whichever script its translation needs. */
    private static Phrase label(Locale locale, String key) {
        String text = Messages.get(locale, key);
        return new Phrase(text,
                PdfFonts.forText(text, FontFactory.HELVETICA, 5.5f, Font.NORMAL, Color.GRAY));
    }

    private static PdfPCell borderless(Phrase phrase) {
        PdfPCell cell = new PdfPCell(phrase);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(1f);
        return cell;
    }
}
