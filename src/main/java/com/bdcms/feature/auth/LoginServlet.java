package com.bdcms.feature.auth;

import com.bdcms.shared.web.BaseServlet;
import com.bdcms.shared.web.SessionUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
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
