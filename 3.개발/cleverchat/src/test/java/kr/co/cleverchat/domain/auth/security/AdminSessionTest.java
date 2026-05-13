package kr.co.cleverchat.domain.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AdminSessionTest {

    @Test
    void normalizesRolesToUppercaseRolePrefix() {
        AdminSession session = session(Set.of(" admin ", "ROLE_operator", "", "  "));

        assertThat(session.getRoles()).containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_OPERATOR");
        assertThat(session.hasRole("admin")).isTrue();
        assertThat(session.hasRole("ROLE_OPERATOR")).isTrue();
        assertThat(session.hasRole("viewer")).isFalse();
    }

    @Test
    void hasAnyRoleMatchesAnyNormalizedRole() {
        AdminSession session = session(Set.of("ROLE_ADMIN"));

        assertThat(session.hasAnyRole("operator", "admin")).isTrue();
        assertThat(session.hasAnyRole("operator", "viewer")).isFalse();
        assertThat(session.hasAnyRole((String[]) null)).isFalse();
    }

    private AdminSession session(Set<String> roles) {
        return new AdminSession(1L, "admin", "관리자", roles, false, LocalDateTime.now());
    }
}
