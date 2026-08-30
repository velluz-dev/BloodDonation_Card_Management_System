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
            default -> Flash.error(request, msg(request, "flash.unknownAction"));
        }
        redirect(request, response, "/app/admin/users");
    }

    private void create(HttpServletRequest request) {
        String username = param(request, "username");
        String fullName = param(request, "fullName");
        String password = request.getParameter("password");
        Role role = Role.of(param(request, "role", "STAFF"));

        if (!Validate.isUsername(username)) {
            Flash.error(request, msg(request, "flash.usernameFormat"));
            return;
        }
        if (Validate.isBlank(fullName)) {
            Flash.error(request, msg(request, "flash.fullNameRequired"));
            return;
        }
        if (password == null || password.length() < 8) {
            Flash.error(request, msg(request, "flash.passwordTooShort"));
            return;
        }

        Optional<User> created = authService.createUser(username, fullName, role, password);
        if (created.isEmpty()) {
            Flash.error(request, msg(request, "flash.usernameTaken"));
        } else {
            Flash.success(request, msg(request, "flash.accountCreated", username,
                    msg(request, role.getMessageKey())));
        }
    }

    private void setActive(HttpServletRequest request, boolean active) {
        Optional<Long> userId = longParam(request, "userId");
        if (userId.isEmpty()) {
            Flash.error(request, msg(request, "flash.noAccountSelected"));
            return;
        }
        SessionUser me = currentUser(request);
        if (!active && me != null && me.userId() == userId.get()) {
            // Locking yourself out is not a scenario worth supporting.
            Flash.error(request, msg(request, "flash.cannotDisableSelf"));
            return;
        }
        authService.setActive(userId.get(), active);
        Flash.success(request, msg(request, active ? "flash.accountEnabled" : "flash.accountDisabled"));
    }

    private void resetPassword(HttpServletRequest request) {
        Optional<Long> userId = longParam(request, "userId");
        String password = request.getParameter("password");
        if (userId.isEmpty() || password == null || password.length() < 8) {
            Flash.error(request, msg(request, "flash.newPasswordRequired"));
            return;
        }
        authService.resetPassword(userId.get(), password);
        Flash.success(request, msg(request, "flash.passwordReset"));
    }
}
