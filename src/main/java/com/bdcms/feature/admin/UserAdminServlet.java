package com.bdcms.feature.admin;

import com.bdcms.feature.auth.AuthService;
import com.bdcms.feature.auth.Role;
import com.bdcms.feature.auth.User;
import com.bdcms.shared.util.Validate;
import com.bdcms.shared.web.BaseServlet;
import com.bdcms.shared.web.Flash;
import com.bdcms.shared.web.SessionUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

/**
 * Staff account management (FR-1), for administrators.
 *
 * <p>Accounts are disabled rather than deleted. Every donation and every
 * verification attempt names the staff account responsible, so removing a user
 * row would break the audit trail the system exists to provide.
 */
@WebServlet(name = "userAdminServlet", urlPatterns = "/app/admin/users")
public class UserAdminServlet extends BaseServlet {

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!requireAdmin(request, response)) {
            return;
        }
        request.setAttribute("users", authService.listUsers());
        request.setAttribute("roles", Role.values());
        render(request, response, "admin/users.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        if (!requireAdmin(request, response)) {
            return;
        }
        String action = param(request, "action", "create");

        switch (action) {
            case "create" -> create(request);
            case "disable" -> setActive(request, false);
            case "enable" -> setActive(request, true);
            case "resetPassword" -> resetPassword(request);
            default -> Flash.error(request, "Unknown action.");
        }
        redirect(request, response, "/app/admin/users");
    }

    private void create(HttpServletRequest request) {
        String username = param(request, "username");
        String fullName = param(request, "fullName");
        String password = request.getParameter("password");
        Role role = Role.of(param(request, "role", "STAFF"));

        if (!Validate.isUsername(username)) {
            Flash.error(request, "Username must be 3-40 letters, digits, dots, hyphens or underscores.");
            return;
        }
        if (Validate.isBlank(fullName)) {
            Flash.error(request, "Full name is required.");
            return;
        }
        if (password == null || password.length() < 8) {
            Flash.error(request, "Password must be at least 8 characters.");
            return;
        }

        Optional<User> created = authService.createUser(username, fullName, role, password);
        if (created.isEmpty()) {
            Flash.error(request, "That username is already taken.");
        } else {
            Flash.success(request, "Account '" + username + "' created with the "
                    + role.getLabel() + " role.");
        }
    }

    private void setActive(HttpServletRequest request, boolean active) {
        Optional<Long> userId = longParam(request, "userId");
        if (userId.isEmpty()) {
            Flash.error(request, "No account selected.");
            return;
        }
        SessionUser me = currentUser(request);
        if (!active && me != null && me.userId() == userId.get()) {
            // Locking yourself out is not a scenario worth supporting.
            Flash.error(request, "You cannot disable the account you are signed in with.");
            return;
        }
        authService.setActive(userId.get(), active);
        Flash.success(request, active ? "Account enabled." : "Account disabled.");
    }

    private void resetPassword(HttpServletRequest request) {
        Optional<Long> userId = longParam(request, "userId");
        String password = request.getParameter("password");
        if (userId.isEmpty() || password == null || password.length() < 8) {
            Flash.error(request, "A new password of at least 8 characters is required.");
            return;
        }
        authService.resetPassword(userId.get(), password);
        Flash.success(request, "Password reset. Ask the user to change it after signing in.");
    }
}
