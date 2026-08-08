package com.bdcms.shared.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.io.Serializable;

/**
 * The authenticated staff member, as stored in the HTTP session.
 *
 * <p>Deliberately a small immutable snapshot rather than the full {@code User}
 * entity: the session should not carry a password hash, and JSPs only ever need
 * the display name and role.
 *
 * <p>Every donation and every verification attempt records {@link #userId()},
 * which is what satisfies the auditability requirement.
 */
public record SessionUser(long userId, String username, String fullName, String role)
        implements Serializable {

    public static final String SESSION_KEY = "bdcms.user";

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    // ---- JavaBean accessors for JSP/EL -------------------------------------
    //
    // Jakarta EL 5.0, which Tomcat 10.1 implements, does not resolve record
    // components as bean properties - that arrived in EL 6.0. Without these,
    // ${currentUser.fullName} throws at render time even though the page
    // compiles perfectly. The record accessors remain the ones Java code uses.

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    /** Places this user on a brand new session (session-fixation defence). */
    public void bindTo(HttpServletRequest request) {
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        request.getSession(true).setAttribute(SESSION_KEY, this);
    }

    /** The signed-in user, or {@code null} when the session is absent or expired. */
    public static SessionUser from(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_KEY);
        return value instanceof SessionUser user ? user : null;
    }
}
