package com.bdcms.feature.admin;

import com.bdcms.feature.eligibility.EligibilityService;
import com.bdcms.shared.config.SettingsService;
import com.bdcms.shared.db.Tx;
import com.bdcms.shared.web.BaseServlet;
import com.bdcms.shared.web.Flash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Administrator screen for the runtime-configurable rules.
 *
 * <p>This is what makes the maintainability requirement real rather than
 * aspirational: the minimum donation interval is edited here, in the database,
 * and {@code EligibilityService} picks the new value up on its next read. No
 * code changes, no rebuild, no redeploy.
 *
 * <p>Mapped under {@code /app/admin}, so {@link com.bdcms.shared.web.AuthFilter}
 * already restricts it to administrators; {@link #requireAdmin} repeats the
 * check because a rule this consequential should not depend on a URL prefix
 * staying where it is.
 */
@WebServlet(name = "settingsServlet", urlPatterns = "/app/admin/settings")
public class SettingsServlet extends BaseServlet {

    private final SettingsService settingsService = new SettingsService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!requireAdmin(request, response)) {
            return;
        }
        Map<String, String> settings = Tx.read(settingsService::all);
        request.setAttribute("settings", settings);
        request.setAttribute("intervalKey", SettingsService.MIN_DONATION_INTERVAL_DAYS);
        request.setAttribute("defaultInterval", EligibilityService.DEFAULT_INTERVAL_DAYS);
        render(request, response, "admin/settings.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        if (!requireAdmin(request, response)) {
            return;
        }

        String key = param(request, "settingKey");
        String value = param(request, "settingValue");

        if (key == null || value == null) {
            Flash.error(request, msg(request, "flash.settingAndValueRequired"));
            redirect(request, response, "/app/admin/settings");
            return;
        }

        // The interval drives donor safety: reject anything that is not a
        // sensible number of days rather than storing it and failing later.
        if (SettingsService.MIN_DONATION_INTERVAL_DAYS.equals(key)) {
            int days;
            try {
                days = Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                Flash.error(request, msg(request, "flash.intervalWholeNumber"));
                redirect(request, response, "/app/admin/settings");
                return;
            }
            if (days < 1 || days > 365) {
                Flash.error(request, msg(request, "flash.intervalRange"));
                redirect(request, response, "/app/admin/settings");
                return;
            }
        }

        Tx.inTransaction(conn -> {
            settingsService.save(conn, key, value);
            return null;
        });
        settingsService.invalidate();

        Flash.success(request, msg(request, "flash.settingUpdated", key, value));
        redirect(request, response, "/app/admin/settings");
    }
}
