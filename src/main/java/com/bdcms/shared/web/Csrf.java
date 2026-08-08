package com.bdcms.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Per-session CSRF token.
 *
 * <p>Recording a donation and deactivating a card are state-changing POSTs made
 * by an authenticated member of staff, which is exactly the shape of request a
 * cross-site forgery targets. Every form in this application carries the token
 * below as a hidden field and {@link CsrfFilter} rejects POSTs without it.
 */
public final class Csrf {

    public static final String FIELD = "_csrf";
    private static final String SESSION_KEY = "bdcms.csrf";
    private static final SecureRandom RANDOM = new SecureRandom();

    private Csrf() {
    }

    /** The session's token, generated on first use. */
    public static String token(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        Object existing = session.getAttribute(SESSION_KEY);
        if (existing instanceof String token) {
            return token;
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        session.setAttribute(SESSION_KEY, token);
        return token;
    }

    /** True when the submitted token matches the session's. */
    public static boolean isValid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object expected = session.getAttribute(SESSION_KEY);
        String submitted = request.getParameter(FIELD);
        if (!(expected instanceof String expectedToken) || submitted == null) {
            return false;
        }
        return constantTimeEquals(expectedToken, submitted);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}
