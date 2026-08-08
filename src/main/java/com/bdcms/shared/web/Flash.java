package com.bdcms.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * One-shot messages that survive a redirect.
 *
 * <p>Write operations in this system follow POST-redirect-GET so that a browser
 * refresh cannot record a second donation. The success or error banner therefore
 * has to outlive the POST request, which is what this class provides: the value
 * is stored on the session and removed the first time a page reads it.
 */
public final class Flash {

    private static final String SUCCESS = "bdcms.flash.success";
    private static final String ERROR = "bdcms.flash.error";

    private Flash() {
    }

    public static void success(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute(SUCCESS, message);
    }

    public static void error(HttpServletRequest request, String message) {
        request.getSession(true).setAttribute(ERROR, message);
    }

    /**
     * Moves any pending messages from the session onto the request, so the
     * layout JSP can render them, then clears them.
     */
    public static void expose(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return;
        }
        moveToRequest(request, session, SUCCESS, "flashSuccess");
        moveToRequest(request, session, ERROR, "flashError");
    }

    private static void moveToRequest(HttpServletRequest request, HttpSession session,
                                      String sessionKey, String requestKey) {
        Object value = session.getAttribute(sessionKey);
        if (value != null) {
            request.setAttribute(requestKey, value);
            session.removeAttribute(sessionKey);
        }
    }
}
