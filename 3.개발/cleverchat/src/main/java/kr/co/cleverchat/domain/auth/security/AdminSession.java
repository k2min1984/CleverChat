package kr.co.cleverchat.domain.auth.security;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public class AdminSession implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    public static final String SESSION_KEY = "CLEVERCHAT_ADMIN_SESSION";

    private final Long id;
    private final String username;
    private final String displayName;
    private final Set<String> roles;
    private final boolean mustChangePassword;
    private final LocalDateTime loginAt;

    public AdminSession(
            Long id,
            String username,
            String displayName,
            Set<String> roles,
            boolean mustChangePassword,
            LocalDateTime loginAt) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.roles = Collections.unmodifiableSet(normalizeRoles(roles));
        this.mustChangePassword = mustChangePassword;
        this.loginAt = loginAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public LocalDateTime getLoginAt() {
        return loginAt;
    }

    public boolean hasRole(String role) {
        return roles.contains(normalizeRole(role));
    }

    public boolean hasAnyRole(String... roles) {
        if (roles == null) {
            return false;
        }
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> normalizeRoles(Set<String> roles) {
        Set<String> normalized = new LinkedHashSet<>();
        if (roles == null) {
            return normalized;
        }
        for (String role : roles) {
            normalized.add(normalizeRole(role));
        }
        normalized.remove("");
        return normalized;
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.startsWith("ROLE_") ? normalized : "ROLE_" + normalized;
    }
}
