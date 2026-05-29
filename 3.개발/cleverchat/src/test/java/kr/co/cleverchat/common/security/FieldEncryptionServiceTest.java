package kr.co.cleverchat.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class FieldEncryptionServiceTest {

    @Test
    void encryptUsesRandomIvAndDecrypts() {
        FieldEncryptionService service =
                serviceWithKey("test-v1", "12345678901234567890123456789012");

        var first = service.encrypt("hello-secret");
        var second = service.encrypt("hello-secret");

        assertThat(first.ciphertext()).isNotEqualTo(second.ciphertext());
        assertThat(first.keyId()).isEqualTo("test-v1");
        assertThat(first.version()).isEqualTo(FieldEncryptionService.ENCRYPTION_VERSION);
        assertThat(service.decryptOrFallback(first.ciphertext(), null)).isEqualTo("hello-secret");
        assertThat(service.decryptOrFallback(second.ciphertext(), null)).isEqualTo("hello-secret");
    }

    @Test
    void corruptedCiphertextThrowsSanitizedBusinessException() {
        FieldEncryptionService service =
                serviceWithKey("test-v1", "12345678901234567890123456789012");

        assertThatThrownBy(() -> service.decryptOrFallback("v1:broken", "plain-secret"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DATA_DECRYPTION_FAILED);

        assertThatThrownBy(() -> service.decryptOrFallback("v1:broken", "plain-secret"))
                .hasMessageContaining("Encrypted field cannot be read.")
                .hasMessageNotContaining("plain-secret")
                .hasMessageNotContaining("12345678901234567890123456789012");
    }

    @Test
    void decryptsPreviousKeyCiphertextByKeyId() {
        FieldEncryptionService oldService =
                serviceWithKey("old-v1", "11111111111111111111111111111111");
        var oldEncrypted = oldService.encrypt("legacy-secret");
        String activeKey =
                Base64.getEncoder()
                        .encodeToString(
                                "22222222222222222222222222222222"
                                        .getBytes(StandardCharsets.UTF_8));
        String previousKey =
                Base64.getEncoder()
                        .encodeToString(
                                "11111111111111111111111111111111"
                                        .getBytes(StandardCharsets.UTF_8));
        FieldEncryptionService service =
                new FieldEncryptionService(
                        activeKey, "new-v1", "old-v1:" + previousKey, new MockEnvironment());

        assertThat(service.decryptOrFallback(oldEncrypted.ciphertext(), oldEncrypted.keyId(), null))
                .isEqualTo("legacy-secret");
        assertThat(service.encrypt("new-secret").keyId()).isEqualTo("new-v1");
    }

    @Test
    void unknownKeyIdThrowsSanitizedBusinessException() {
        FieldEncryptionService service =
                serviceWithKey("test-v1", "12345678901234567890123456789012");
        var encrypted = service.encrypt("plain-secret");

        assertThatThrownBy(
                        () ->
                                service.decryptOrFallback(
                                        encrypted.ciphertext(), "missing-v1", "plain-secret"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DATA_DECRYPTION_FAILED);
    }

    @Test
    void invalidKeyLengthFailsFast() {
        assertThatThrownBy(() -> serviceWithKey("bad-v1", "short"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void stageAndProdRequireExplicitKey() {
        MockEnvironment environment =
                new MockEnvironment().withProperty("spring.profiles.active", "prod");
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> new FieldEncryptionService("", "prod-v1", "", environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CLEVERCHAT_FIELD_ENCRYPTION_KEY_BASE64");
    }

    @Test
    void devAndTestCanUseLocalFallbackKey() {
        FieldEncryptionService service =
                new FieldEncryptionService("", "local-v1", "", new MockEnvironment());

        var encrypted = service.encrypt("local plaintext");

        assertThat(service.decryptOrFallback(encrypted.ciphertext(), null))
                .isEqualTo("local plaintext");
    }

    private FieldEncryptionService serviceWithKey(String keyId, String rawKey) {
        String keyBase64 =
                Base64.getEncoder().encodeToString(rawKey.getBytes(StandardCharsets.UTF_8));
        return new FieldEncryptionService(keyBase64, keyId, "", new MockEnvironment());
    }
}
