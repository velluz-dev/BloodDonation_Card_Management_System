package com.bdcms.shared.config;

import com.bdcms.shared.db.DaoException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Reads and writes the {@code settings} table.
 *
 * <p>Lives in {@code shared} rather than in a feature package because more than
 * one feature needs it (eligibility reads the interval, the admin screen edits
 * it) and it belongs to no single vertical slice.
 */
public class SettingsDao {

    public Optional<String> find(Connection conn, String key) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not read setting " + key, e);
        }
    }

    public Map<String, String> findAll(Connection conn) {
        String sql = "SELECT setting_key, setting_value FROM settings ORDER BY setting_key";
        Map<String, String> all = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                all.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
            return all;
        } catch (SQLException e) {
            throw new DaoException("Could not read settings", e);
        }
    }

    public void upsert(Connection conn, String key, String value) {
        String sql = "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE setting_value = VALUES(setting_value)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Could not save setting " + key, e);
        }
    }
}
