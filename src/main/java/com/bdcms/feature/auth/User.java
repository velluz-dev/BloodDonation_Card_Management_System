package com.bdcms.feature.auth;

import java.time.LocalDateTime;

/**
 * A staff or administrator account (FR-1).
 *
 * <p>Carries {@code passwordHash} because {@link AuthService} needs it to
 * verify a login. It is deliberately never placed on the session or exposed to
 * a JSP - {@link com.bdcms.shared.web.SessionUser} is the sanitised view used
 * for that.
 */
public class User {

    private long userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private Role role = Role.STAFF;
    private boolean active = true;
    private LocalDateTime createdAt;

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
