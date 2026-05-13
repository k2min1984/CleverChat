package kr.co.cleverchat.domain.auth.security;

import java.util.LinkedHashSet;
import java.util.Set;
import kr.co.cleverchat.domain.auth.model.UserAccount;

public class AuthenticatedUser {

    private final Long id;
    private final String username;
    private final String displayName;
    private final Set<String> roles;
    private final boolean mustChangePassword;

    public AuthenticatedUser(UserAccount account) {
        this.id = account.getId();
        this.username = account.getUsername();
        this.displayName = account.getDisplayName();
        this.roles = normalizeRoles(account.getRoles() == null ? Set.of() : new LinkedHashSet<>(account.getRoles()));
        this.mustChangePassword = account.isMustChangePassword();
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

    private Set<String> normalizeRoles(Set<String> roles) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String role : roles) {
            if (role != null && !role.isBlank()) {
                normalized.add(role.trim());
            }
        }
        return normalized;
    }
}
