package com.bdcms.feature.donor;

import com.bdcms.shared.web.BaseServlet;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Streams a donor's photograph (FR-2, and the visual half of FR-4).
 *
 * <p>Photographs live outside the web root, so this is the only route to them.
 * That gives the system two things a static directory could not: the image is
 * behind {@link com.bdcms.shared.web.AuthFilter} like every other donor datum,
 * and an uploaded file can never be interpreted by the container as a JSP.
 */
@WebServlet(name = "donorPhotoServlet", urlPatterns = "/app/donors/photo")
public class DonorPhotoServlet extends BaseServlet {

    private final DonorService donorService = new DonorService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Optional<Long> donorId = longParam(request, "donorId");
        if (donorId.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "donorId is required");
            return;
        }

        Optional<Donor> donor = donorService.findById(donorId.get());
        if (donor.isEmpty() || !donor.get().isHasPhoto()) {
            sendPlaceholder(response);
            return;
        }

        Path file = PhotoStore.resolve(donor.get().getPhotoPath());
        if (file == null || !Files.isRegularFile(file)) {
            sendPlaceholder(response);
            return;
        }

        response.setContentType(PhotoStore.contentTypeOf(donor.get().getPhotoPath()));
        response.setContentLengthLong(Files.size(file));
        // Private: a shared proxy must not retain donor images.
        response.setHeader("Cache-Control", "private, max-age=300");
        try (OutputStream out = response.getOutputStream()) {
            Files.copy(file, out);
        }
    }

    /**
     * A donor with no photograph still has to render. Rather than a broken
     * image on the verification screen - where a missing photo is a safety
     * signal, not a cosmetic issue - send an explicit grey placeholder.
     */
    private void sendPlaceholder(HttpServletResponse response) throws IOException {
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="240" height="300" viewBox="0 0 240 300">
                  <rect width="240" height="300" fill="#e9ecef"/>
                  <circle cx="120" cy="115" r="45" fill="#adb5bd"/>
                  <path d="M40 300c0-46 36-80 80-80s80 34 80 80z" fill="#adb5bd"/>
                  <text x="120" y="285" font-family="sans-serif" font-size="15"
                        fill="#495057" text-anchor="middle">No photograph</text>
                </svg>
                """;
        response.setContentType("image/svg+xml");
        response.setHeader("Cache-Control", "private, max-age=600");
        response.getWriter().write(svg);
    }
}
