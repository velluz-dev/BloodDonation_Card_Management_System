package com.bdcms.shared.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * Gate for the whole authenticated area.
 *
 * <p>Every functional screen lives under {@code /app/*}, so a single mapping
 * covers the entire system: there is no way to add a new servlet that is
 * accidentally left unprotected, provided it is mapped under {@code /app}.
 *
 * <p>Anonymous users are redirected to the login page with the originally
 * requested URL preserved, so they land where they meant to go after signing in.
 * Administrator-only areas are additionally checked here rather than in JSPs -
 * hiding a menu item is presentation, not authorisation.
 */
public class AuthFilter implements Filter {

    /** Path prefixes, relative to {@code /app}, that only ADMIN may open. */
    private static final List<String> ADMIN_ONLY = List.of("/admin", "/reports", "/users");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        SessionUser user = SessionUser.from(req);
        if (user == null) {
            redirectToLogin(req, res);
            return;
        }

        String path = req.getPathInfo() == null ? "" : req.getPathInfo();
        if (isAdminOnly(path) && !user.isAdmin()) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "This area is restricted to administrators.");
            return;
        }

        // Never cache an authenticated page: a logged-out browser must not be
        // able to reveal donor data with the Back button.
        res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        res.setHeader("Pragma", "no-cache");

        chain.doFilter(request, response);
    }

    private boolean isAdminOnly(String pathInfo) {
        return ADMIN_ONLY.stream().anyMatch(pathInfo::startsWith);
    }

    private void redirectToLogin(HttpServletRequest req, HttpServletResponse res) throws IOException {
        if (isAjax(req)) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired");
            return;
        }
        String target = req.getRequestURI();
        if (req.getQueryString() != null) {
            target += "?" + req.getQueryString();
        }
        String encoded = java.net.URLEncoder.encode(target, java.nio.charset.StandardCharsets.UTF_8);
        res.sendRedirect(req.getContextPath() + "/login?expired=1&next=" + encoded);
    }

    private boolean isAjax(HttpServletRequest req) {
        return "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));
    }
}
