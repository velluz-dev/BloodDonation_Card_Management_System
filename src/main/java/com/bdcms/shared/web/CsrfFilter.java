package com.bdcms.shared.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Rejects state-changing requests that do not carry the session's CSRF token.
 *
 * <p>Only POSTs are checked - GETs in this application never modify state.
 *
 * <p><strong>Multipart is deliberately skipped here.</strong> Reading a
 * parameter from a {@code multipart/form-data} body inside a filter consumes
 * the stream before the servlet's {@code @MultipartConfig} parser can see it,
 * which would break the donor photo upload. Multipart servlets therefore
 * validate the token themselves via {@link BaseServlet#requireCsrf}, once the
 * container has parsed the parts.
 */
public class CsrfFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if ("POST".equalsIgnoreCase(req.getMethod()) && !isMultipart(req) && !Csrf.isValid(req)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Invalid or missing security token. Please reload the page and try again.");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null
                && contentType.toLowerCase().startsWith("multipart/form-data");
    }
}
