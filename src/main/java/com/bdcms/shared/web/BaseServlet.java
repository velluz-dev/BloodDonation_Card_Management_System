package com.bdcms.shared.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Shared behaviour for every controller in the system.
 *
 * <p>Keeps three things out of the individual feature servlets: rendering a JSP
 * (always through {@code /WEB-INF/views}, so no view can be requested directly
 * by URL), reading and validating request parameters, and role checks.
 */
public abstract class BaseServlet extends HttpServlet {

    /** Forwards to a view under {@code /WEB-INF/views}, exposing any flash messages. */
    protected void render(HttpServletRequest request, HttpServletResponse response, String view)
            throws ServletException, IOException {
        Flash.expose(request);
        request.setAttribute("csrfToken", Csrf.token(request));
        request.setAttribute("currentUser", SessionUser.from(request));
        // ISO today, for pre-filling and bounding <input type="date"> fields.
        request.setAttribute("today", LocalDate.now().toString());
        // The sidebar shows the centre name on every page, so it is exposed here
        // rather than by each servlet that happens to render a shell.
        request.setAttribute("centreName", com.bdcms.shared.config.AppConfig.defaultCentre());
        // Wall-clock time, for the "looked up at" stamp on the verification screen.
        request.setAttribute("nowTime", java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")));
        request.getRequestDispatcher("/WEB-INF/views/" + view).forward(request, response);
    }

    /** Redirects to a context-relative path, e.g. {@code redirect(res, "/app/donors")}. */
    protected void redirect(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        response.sendRedirect(request.getContextPath() + path);
    }

    /** The signed-in staff member. Never null below {@code /app} - AuthFilter guarantees it. */
    protected SessionUser currentUser(HttpServletRequest request) {
        return SessionUser.from(request);
    }

    /** Sends 403 and returns false when the user does not hold the ADMIN role. */
    protected boolean requireAdmin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        SessionUser user = currentUser(request);
        if (user == null || !user.isAdmin()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Administrator role required.");
            return false;
        }
        return true;
    }

    /**
     * CSRF check for {@code multipart/form-data} servlets, which {@link CsrfFilter}
     * skips. Call this first in {@code doPost}. Returns false after sending 403.
     */
    protected boolean requireCsrf(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        if (Csrf.isValid(request)) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN,
                "Invalid or missing security token. Please reload the page and try again.");
        return false;
    }

    // ---- parameter helpers -------------------------------------------------

    /** Trimmed parameter, or null when absent or blank. */
    protected String param(HttpServletRequest request, String name) {
        String value = request.getParameter(name);
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected String param(HttpServletRequest request, String name, String fallback) {
        String value = param(request, name);
        return value == null ? fallback : value;
    }

    protected Optional<Long> longParam(HttpServletRequest request, String name) {
        String value = param(request, name);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    protected int intParam(HttpServletRequest request, String name, int fallback) {
        String value = param(request, name);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    protected Optional<LocalDate> dateParam(HttpServletRequest request, String name) {
        String value = param(request, name);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDate.parse(value));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
