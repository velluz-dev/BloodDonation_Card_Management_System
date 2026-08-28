package com.bdcms.feature.auth;

import com.bdcms.shared.config.AppConfig;
import com.bdcms.shared.web.BaseServlet;
import com.bdcms.shared.web.SessionUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Staff sign-in (FR-1).
 *
 * <p>Sits outside {@code /app}, so {@link com.bdcms.shared.web.AuthFilter} does
 * not guard it. On success the user is bound to a brand new session (defeating
 * session fixation) and sent either to the page they originally asked for or to
 * the dashboard.
 */
@WebServlet(name = "loginServlet", urlPatterns = "/login")
public class LoginServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (SessionUser.from(request) != null) {
            redirect(request, response, "/app/dashboard");
            return;
        }
        if (request.getParameter("expired") != null) {
            request.setAttribute("infoMessage",
                    "Your session has ended. Please sign in again.");
        }
        request.setAttribute("next", param(request, "next"));
        exposeDemoAccounts(request);
        render(request, response, "auth/login.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String username = param(request, "username");
        String password = request.getParameter("password");
        String next = param(request, "next");

        Optional<User> authenticated = authService.login(username, password);

        if (authenticated.isEmpty()) {
            // One message for every failure mode: a specific one would tell an
            // attacker whether the username exists or the account is disabled.
            request.setAttribute("errorMessage", "Incorrect username or password.");
            request.setAttribute("username", username);
            request.setAttribute("next", next);
            exposeDemoAccounts(request);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            render(request, response, "auth/login.jsp");
            return;
        }

        User user = authenticated.get();
        new SessionUser(user.getUserId(), user.getUsername(), user.getFullName(), user.getRole().name())
                .bindTo(request);

        redirect(request, response, safeNext(request, next));
    }

    /**
     * Puts the seeded demonstration accounts on the sign-in page, when the
     * build is configured to show them.
     *
     * <p>They are listed here rather than typed into the JSP so that the
     * usernames, passwords and roles have a single source, and adding a seeded
     * account means editing one list next to the SQL that creates it.
     *
     * <p>These are the accounts in {@code db/02_seed.sql}. If that file changes,
     * this list must change with it - {@code PasswordsTest.seededAdminHashIsValid}
     * is the guard that catches the admin password drifting apart from its hash.
     */
    private void exposeDemoAccounts(HttpServletRequest request) {
        if (!AppConfig.showDemoCredentials()) {
            return;
        }
        request.setAttribute("demoAccounts", List.of(
                new DemoAccount("admin", "Admin@123", "Administrator",
                        "Everything below, plus reports, settings, staff accounts and the audit log"),
                new DemoAccount("staff", "Staff@123", "Staff",
                        "Register donors, issue cards, verify and record donations"),
                new DemoAccount("nurse", "Nurse@123", "Staff",
                        "The same as staff - a second account, to show who did what")));
    }

    /**
     * One row of the demonstration-accounts panel.
     *
     * <p>A class with JavaBean getters rather than a record: Tomcat 10.1
     * implements Jakarta EL 5.0, which cannot read record components as
     * properties. See {@code com.bdcms.shared.web.SessionUser}.
     */
    public static class DemoAccount {
        private final String username;
        private final String password;
        private final String role;
        private final String can;

        DemoAccount(String username, String password, String role, String can) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.can = can;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getRole() {
            return role;
        }

        public String getCan() {
            return can;
        }

        public boolean isAdmin() {
            return "Administrator".equals(role);
        }
    }

    /**
     * Only ever redirects within this application: an attacker-supplied
     * {@code next} pointing at another host would turn the login page into an
     * open redirect.
     */
    private String safeNext(HttpServletRequest request, String next) {
        if (next == null) {
            return "/app/dashboard";
        }
        String contextPath = request.getContextPath();
        if (!next.startsWith(contextPath + "/") || next.startsWith("//")) {
            return "/app/dashboard";
        }
        String withinApp = next.substring(contextPath.length());
        return withinApp.startsWith("/app/") ? withinApp : "/app/dashboard";
    }
}
