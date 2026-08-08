package com.bdcms.feature.card;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;

/**
 * Encodes a card number as a QR code (FR-3).
 *
 * <p>The QR payload is the card number and nothing else - no name, no national
 * ID. A donor card is carried in a wallet and can be photographed by anyone
 * standing nearby; encoding personal data into it would leak that data to a
 * bystander with a phone. The card number alone is useless without a login to
 * this system.
 *
 * <p>Error correction is set to {@code M} (about 15% recoverable) because a
 * printed card that lives in a wallet gets creased and scuffed, and a scanner
 * at the counter has to read it first time.
 */
public final class QrCodeGenerator {

    private static final int DEFAULT_SIZE = 260;

    private QrCodeGenerator() {
    }

    /** PNG bytes of the QR code for {@code cardNo}, at the default size. */
    public static byte[] png(String cardNo) {
        return png(cardNo, DEFAULT_SIZE);
    }

    /** PNG bytes of the QR code for {@code cardNo}, {@code size} pixels square. */
    public static byte[] png(String cardNo, int size) {
        if (cardNo == null || cardNo.isBlank()) {
            throw new IllegalArgumentException("Card number is required to build a QR code");
        }
        int pixels = Math.max(80, Math.min(size, 1200));

        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        try {
            BitMatrix matrix = new QRCodeWriter()
                    .encode(cardNo, BarcodeFormat.QR_CODE, pixels, pixels, hints);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Could not generate QR code for " + cardNo, e);
        }
    }
}
