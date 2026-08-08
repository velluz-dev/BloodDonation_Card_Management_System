package com.bdcms.feature.auth;

import com.bdcms.shared.db.DaoException;
import com.bdcms.shared.util.Dates;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All SQL against the {@code users} table (FR-1).
 *
 * <p>This is the only class permitted to touch that table. Every statement is a
 * {@link PreparedStatement} with bound parameters, which is what makes SQL
 * injection structurally impossible rather than merely unlikely.
 */
public class UserDao {

    private static final String COLUMNS =
            "user_id, username, password_hash, full_name, role, is_active, created_at";

    public Optional<User> findByUsername(Connection conn, String username) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not look up user " + username, e);
        }
    }

    public Optional<User> findById(Connection conn, long userId) {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not load user " + userId, e);
        }
    }

    public List<User> findAll(Connection conn) {
        String sql = "SELECT " + COLUMNS + " FROM users ORDER BY role, username";
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(map(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new DaoException("Could not list users", e);
        }
    }

    public long insert(Connection conn, User user) {
        String sql = "INSERT INTO users (username, password_hash, full_name, role, is_active) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole().name());
            ps.setBoolean(5, user.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long id = keys.getLong(1);
                user.setUserId(id);
                return id;
            }
        } catch (SQLException e) {
            throw new DaoException("Could not create user " + user.getUsername(), e);
        }
    }

    public void setActive(Connection conn, long userId, boolean active) {
        String sql = "UPDATE users SET is_active = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Could not change status of user " + userId, e);
        }
    }

    public void updatePassword(Connection conn, long userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Could not change password for user " + userId, e);
        }
    }

    public boolean usernameExists(Connection conn, String username) {
        String sql = "SELECT 1 FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not check username " + username, e);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(Role.of(rs.getString("role")));
        user.setActive(rs.getBoolean("is_active"));
        user.setCreatedAt(Dates.dateTime(rs, "created_at"));
        return user;
    }
}
