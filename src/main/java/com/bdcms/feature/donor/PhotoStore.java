package com.bdcms.feature.donor;

import com.bdcms.shared.config.AppConfig;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores donor photographs on disk and hands back the relative path that goes
 * into {@code donors.photo_path}.
 *
 * <p>Three deliberate choices here, all of them security rather than
 * convenience:
 * <ul>
 *   <li>The filename is generated. A client-supplied name could contain
 *       {@code ../} and escape the upload directory.</li>
 *   <li>The type is decided by inspecting the first bytes of the file, not by
 *       trusting the browser's {@code Content-Type} header.</li>
 *   <li>Files land outside the web root and are served by
 *       {@link DonorPhotoServlet}, so an uploaded file can never be executed
 *       by the container as a JSP.</li>
 * </ul>
 */
public final class PhotoStore {

    /** Thrown when an upload is missing, too large, or not a real image. */
    public static class InvalidPhotoException extends RuntimeException {
        public InvalidPhotoException(String message) {
            super(message);
        }
    }

    private PhotoStore() {
    }

    /** True when the part actually carries a file. */
    public static boolean isPresent(Part part) {
        return part != null && part.getSize() > 0;
    }

    /**
     * Writes {@code part} under the configured upload directory and returns its
     * path relative to that directory, e.g. {@code 2026/08/ab12....jpg}.
     */
    public static String save(Part part, long donorId) {
        if (!isPresent(part)) {
            throw new InvalidPhotoException("No photograph was supplied.");
        }
        if (part.getSize() > AppConfig.maxPhotoBytes()) {
            throw new InvalidPhotoException("Photograph must be "
                    + (AppConfig.maxPhotoBytes() / 1024 / 1024) + " MB or smaller.");
        }

        byte[] header = readHeader(part);
        String extension = imageExtension(header);
        if (extension == null) {
            throw new InvalidPhotoException("Photograph must be a JPEG or PNG image.");
        }

        LocalDate today = LocalDate.now();
        String relativeDir = String.format(Locale.ROOT, "%04d/%02d", today.getYear(), today.getMonthValue());
        String filename = donorId + "-" + UUID.randomUUID() + extension;
        String relativePath = relativeDir + "/" + filename;

        Path target = AppConfig.uploadDir().resolve(relativePath).normalize();
        // Belt and braces: the generated name cannot traverse, but verify anyway.
        if (!target.startsWith(AppConfig.uploadDir().normalize())) {
            throw new InvalidPhotoException("Rejected photograph path.");
        }

        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = part.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new InvalidPhotoException("Could not save the photograph: " + e.getMessage());
        }
        return relativePath;
    }

    /** Resolves a stored relative path back to a file, refusing anything outside the upload dir. */
    public static Path resolve(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        Path root = AppConfig.uploadDir().normalize();
        Path resolved = root.resolve(relativePath).normalize();
        return resolved.startsWith(root) ? resolved : null;
    }

    /**
     * Removes a stored photograph from disk.
     *
     * <p>Called after a donor row is deleted. Returns quietly if the file is
     * already gone: the database row is the record that matters, and a missing
     * file must not turn a completed deletion into an error.
     */
    public static boolean delete(String relativePath) {
        Path file = resolve(relativePath);
        if (file == null) {
            return false;
        }
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            // Worth knowing about - an orphaned image of a deleted donor is a
            // privacy matter - but not worth failing the deletion over.
            System.getLogger(PhotoStore.class.getName()).log(System.Logger.Level.WARNING,
                    "Could not delete photograph " + relativePath, e);
            return false;
        }
    }

    /** Content type to serve a stored photo with, derived from its extension. */
    public static String contentTypeOf(String relativePath) {
        return relativePath != null && relativePath.toLowerCase(Locale.ROOT).endsWith(".png")
                ? "image/png"
                : "image/jpeg";
    }

    private static byte[] readHeader(Part part) {
        byte[] header = new byte[8];
        try (InputStream in = part.getInputStream()) {
            int read = in.readNBytes(header, 0, header.length);
            if (read < header.length) {
                throw new InvalidPhotoException("Photograph is not a valid image file.");
            }
        } catch (IOException e) {
            throw new InvalidPhotoException("Could not read the uploaded file.");
        }
        return header;
    }

    /**
     * Magic-number sniffing: JPEG starts {@code FF D8 FF}, PNG starts
     * {@code 89 50 4E 47 0D 0A 1A 0A}. Returns null for anything else.
     */
    private static String imageExtension(byte[] header) {
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
            return ".jpg";
        }
        boolean png = (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                && (header[4] & 0xFF) == 0x0D && (header[5] & 0xFF) == 0x0A
                && (header[6] & 0xFF) == 0x1A && (header[7] & 0xFF) == 0x0A;
        return png ? ".png" : null;
    }
}
