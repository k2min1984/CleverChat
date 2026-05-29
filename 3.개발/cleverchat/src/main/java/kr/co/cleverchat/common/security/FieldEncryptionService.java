package kr.co.cleverchat.common.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class FieldEncryptionService {

    public static final int ENCRYPTION_VERSION = 1;
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final byte[] DEV_TEST_KEY =
            "cleverchat-local-field-key-00001".getBytes(StandardCharsets.UTF_8);

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, SecretKeySpec> keyring;
    private final SecretKeySpec activeKeySpec;
    private final String activeKeyId;

    public FieldEncryptionService(
            @Value("${cleverchat.security.field-encryption.key-base64:}") String keyBase64,
            @Value("${cleverchat.security.field-encryption.key-id:local-v1}") String keyId,
            @Value("${cleverchat.security.field-encryption.previous-keys:}") String previousKeys,
            Environment environment) {
        this.activeKeyId = keyId == null || keyId.isBlank() ? "local-v1" : keyId.trim();
        this.activeKeySpec = new SecretKeySpec(resolveKey(keyBase64, environment), "AES");
        Map<String, SecretKeySpec> resolvedKeyring = new LinkedHashMap<>();
        resolvedKeyring.put(this.activeKeyId, this.activeKeySpec);
        parsePreviousKeys(previousKeys)
                .forEach(
                        (oldKeyId, oldKey) ->
                                resolvedKeyring.put(oldKeyId, new SecretKeySpec(oldKey, "AES")));
        this.keyring = Map.copyOf(resolvedKeyring);
    }

    public EncryptedField encryptNullable(String plaintext) {
        if (plaintext == null) {
            return EncryptedField.empty();
        }
        return encrypt(plaintext);
    }

    public EncryptedField encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, activeKeySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer envelope = ByteBuffer.allocate(iv.length + encrypted.length);
            envelope.put(iv);
            envelope.put(encrypted);
            return new EncryptedField(
                    "v1:" + Base64.getEncoder().encodeToString(envelope.array()),
                    activeKeyId,
                    ENCRYPTION_VERSION);
        } catch (GeneralSecurityException e) {
            throw new BusinessException(
                    ErrorCode.DATA_DECRYPTION_FAILED, "Field encryption failed.");
        }
    }

    public String decryptOrFallback(String ciphertext, String fallback) {
        return decryptOrFallback(ciphertext, activeKeyId, fallback);
    }

    public String decryptOrFallback(String ciphertext, String keyId, String fallback) {
        if (ciphertext == null || ciphertext.isBlank()) {
            return fallback;
        }
        return decrypt(ciphertext, keyId);
    }

    public String activeKeyId() {
        return activeKeyId;
    }

    private String decrypt(String ciphertext, String keyId) {
        try {
            SecretKeySpec decryptKeySpec = keySpecFor(keyId);
            String encoded = ciphertext.startsWith("v1:") ? ciphertext.substring(3) : ciphertext;
            byte[] envelope = Base64.getDecoder().decode(encoded);
            if (envelope.length <= IV_BYTES) {
                throw new GeneralSecurityException("Invalid encrypted field envelope.");
            }
            byte[] iv = Arrays.copyOfRange(envelope, 0, IV_BYTES);
            byte[] encrypted = Arrays.copyOfRange(envelope, IV_BYTES, envelope.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, decryptKeySpec, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new BusinessException(
                    ErrorCode.DATA_DECRYPTION_FAILED, "Encrypted field cannot be read.");
        }
    }

    private byte[] resolveKey(String keyBase64, Environment environment) {
        if (keyBase64 != null && !keyBase64.isBlank()) {
            return decodeKey(keyBase64.trim());
        }
        if (Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equals("stage") || profile.equals("prod"))) {
            throw new IllegalStateException(
                    "CLEVERCHAT_FIELD_ENCRYPTION_KEY_BASE64 is required for stage/prod.");
        }
        return DEV_TEST_KEY;
    }

    private Map<String, byte[]> parsePreviousKeys(String previousKeys) {
        Map<String, byte[]> parsed = new LinkedHashMap<>();
        if (previousKeys == null || previousKeys.isBlank()) {
            return parsed;
        }
        for (String entry : previousKeys.split(";")) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            int separator = entry.indexOf(':');
            if (separator <= 0 || separator == entry.length() - 1) {
                throw new IllegalStateException(
                        "Field encryption previous keys must use keyId:base64 entries.");
            }
            String oldKeyId = entry.substring(0, separator).trim();
            byte[] oldKey = decodeKey(entry.substring(separator + 1).trim());
            if (oldKeyId.isBlank()
                    || oldKeyId.equals(activeKeyId)
                    || parsed.containsKey(oldKeyId)) {
                throw new IllegalStateException("Field encryption key ids must be unique.");
            }
            parsed.put(oldKeyId, oldKey);
        }
        return parsed;
    }

    private SecretKeySpec keySpecFor(String keyId) {
        String resolvedKeyId = keyId == null || keyId.isBlank() ? activeKeyId : keyId.trim();
        SecretKeySpec resolved = keyring.get(resolvedKeyId);
        if (resolved == null) {
            throw new BusinessException(
                    ErrorCode.DATA_DECRYPTION_FAILED, "Encrypted field cannot be read.");
        }
        return resolved;
    }

    private byte[] decodeKey(String keyBase64) {
        byte[] decoded = Base64.getDecoder().decode(keyBase64);
        if (decoded.length != KEY_BYTES) {
            throw new IllegalStateException("Field encryption key must be 32 bytes.");
        }
        return decoded;
    }

    public record EncryptedField(String ciphertext, String keyId, Integer version) {
        public static EncryptedField empty() {
            return new EncryptedField(null, null, null);
        }
    }
}
