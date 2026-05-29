package kr.co.cleverchat.domain.auth.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DefaultAdminSeedPasswordTest {

    private static final String DEFAULT_ADMIN_PASSWORD_HASH =
            "$2a$10$FE8fC2xcY82n3tEJ9cpELOIoQdhCwB1NyQ/t34ILEFZNDRwtbb9s.";

    @Test
    void defaultAdminPasswordHashMatchesAdminPlainText() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        assertThat(passwordEncoder.matches("admin", DEFAULT_ADMIN_PASSWORD_HASH)).isTrue();
    }
}
