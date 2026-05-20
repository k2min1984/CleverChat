package kr.co.cleverchat.domain.auth.security;

import jakarta.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

@Component
public class CsrfTokenIssuer {

    public static final String CSRF_TOKEN_SESSION_ATTRIBUTE = "csrfToken";
    public static final String CSRF_FORM_ID_SESSION_ATTRIBUTE = "csrfFormId";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder TOKEN_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final int CSRF_TOKEN_BYTES = 32;
    private static final int CSRF_FORM_ID_BYTES = 16;

    public void issue(HttpSession session) {
        session.setAttribute(CSRF_TOKEN_SESSION_ATTRIBUTE, generateToken(CSRF_TOKEN_BYTES));
        session.setAttribute(CSRF_FORM_ID_SESSION_ATTRIBUTE, generateToken(CSRF_FORM_ID_BYTES));
    }

    private String generateToken(int byteLength) {
        byte[] tokenBytes = new byte[byteLength];
        SECURE_RANDOM.nextBytes(tokenBytes);
        return TOKEN_ENCODER.encodeToString(tokenBytes);
    }
}
