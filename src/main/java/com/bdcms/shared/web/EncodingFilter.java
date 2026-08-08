package com.bdcms.shared.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Forces UTF-8 on every request and response.
 *
 * <p>Donor names and addresses are stored as {@code utf8mb4}; without this the
 * container would decode form input with the platform default and non-ASCII
 * names would be corrupted before they ever reach a DAO.
 *
 * <p>Declared and ordered first in {@code web.xml} - deliberately not annotated
 * with {@code @WebFilter}, because filter order cannot be expressed in the
 * annotation and this one has to run before any parameter is read.
 */
public class EncodingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        chain.doFilter(request, response);
    }
}
