package kr.co.cleverchat.domain.externalapi;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ExternalApiAuthService {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String API_KEY_HEADER = "X-CleverChat-Api-Key";

    private final ExternalApiProperties properties;

    public ExternalApiAuthService(ExternalApiProperties properties) {
        this.properties = properties;
    }

    public ExternalApiAuthResult authenticate(HttpServletRequest request) {
        if (!properties.isEnabled()) {
            return ExternalApiAuthResult.disabled();
        }
        if (!StringUtils.hasText(properties.getApiKeySha256())) {
            return ExternalApiAuthResult.notConfigured();
        }
        String apiKey = resolveApiKey(request);
        if (!StringUtils.hasText(apiKey)) {
            return ExternalApiAuthResult.unauthorized();
        }
        return matchesHash(apiKey, properties.getApiKeySha256())
                ? ExternalApiAuthResult.authorized()
                : ExternalApiAuthResult.unauthorized();
    }

    private String resolveApiKey(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length()).trim();
        }
        return request.getHeader(API_KEY_HEADER);
    }

    private boolean matchesHash(String rawValue, String expectedHash) {
        byte[] expected = expectedHash.trim().toLowerCase().getBytes(StandardCharsets.UTF_8);
        byte[] actual = sha256(rawValue).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public record ExternalApiAuthResult(Status status) {

        public static ExternalApiAuthResult disabled() {
            return new ExternalApiAuthResult(Status.DISABLED);
        }

        public static ExternalApiAuthResult notConfigured() {
            return new ExternalApiAuthResult(Status.NOT_CONFIGURED);
        }

        public static ExternalApiAuthResult unauthorized() {
            return new ExternalApiAuthResult(Status.UNAUTHORIZED);
        }

        public static ExternalApiAuthResult authorized() {
            return new ExternalApiAuthResult(Status.AUTHORIZED);
        }

        public enum Status {
            DISABLED,
            NOT_CONFIGURED,
            UNAUTHORIZED,
            AUTHORIZED
        }
    }
}
