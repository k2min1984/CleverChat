package kr.co.cleverchat.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import kr.co.cleverchat.common.audit.AuditTrailRecorder;
import kr.co.cleverchat.common.security.FieldEncryptionService;
import kr.co.cleverchat.domain.chatbot.mapper.ChatFailureMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatFeedbackMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatMessageMapper;
import kr.co.cleverchat.domain.chatbot.model.ChatEncryptedFieldRow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

@ExtendWith(MockitoExtension.class)
class ChatFieldEncryptionMaintenanceServiceTest {

    @Mock ChatMessageMapper messageMapper;
    @Mock ChatFeedbackMapper feedbackMapper;
    @Mock ChatFailureMapper failureMapper;
    @Mock AuditTrailRecorder auditTrailRecorder;

    FieldEncryptionService fieldEncryptionService;
    ChatFieldEncryptionMaintenanceService service;

    @BeforeEach
    void setUp() {
        fieldEncryptionService =
                new FieldEncryptionService("", "active-v1", "", new MockEnvironment());
        service =
                new ChatFieldEncryptionMaintenanceService(
                        messageMapper,
                        feedbackMapper,
                        failureMapper,
                        fieldEncryptionService,
                        auditTrailRecorder);
    }

    @Test
    void backfillDryRunDoesNotUpdateRows() {
        when(messageMapper.findContentBackfillCandidates(500))
                .thenReturn(List.of(row(1L, "legacy", null, null)));
        when(feedbackMapper.findCommentBackfillCandidates(500)).thenReturn(List.of());
        when(failureMapper.findReviewCommentBackfillCandidates(500)).thenReturn(List.of());

        var result = service.backfill(true, null);

        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        verify(messageMapper, never())
                .updateEncryptedContent(eq(1L), anyString(), anyString(), anyString(), eq(1));
        assertAudit("FIELD_ENCRYPTION_BACKFILL", true, 500, 1, 0, 0);
    }

    @Test
    void backfillEncryptsLegacyPlaintextRows() {
        when(messageMapper.findContentBackfillCandidates(10))
                .thenReturn(List.of(row(1L, "legacy", null, null)));
        when(feedbackMapper.findCommentBackfillCandidates(10))
                .thenReturn(List.of(row(2L, "comment", null, null)));
        when(failureMapper.findReviewCommentBackfillCandidates(10))
                .thenReturn(List.of(row(3L, "review", null, null)));

        var result = service.backfill(false, 10);

        assertThat(result.scanned()).isEqualTo(3);
        assertThat(result.updated()).isEqualTo(3);
        verify(messageMapper)
                .updateEncryptedContent(
                        eq(1L), eq("[encrypted]"), anyString(), eq("active-v1"), eq(1));
        verify(feedbackMapper)
                .updateEncryptedComment(
                        eq(2L), eq("[encrypted]"), anyString(), eq("active-v1"), eq(1));
        verify(failureMapper)
                .updateEncryptedReviewComment(
                        eq(3L), eq("[encrypted]"), anyString(), eq("active-v1"), eq(1));
        assertAudit("FIELD_ENCRYPTION_BACKFILL", false, 10, 3, 3, 0);
    }

    @Test
    void rotateDryRunDecryptsButDoesNotUpdate() {
        FieldEncryptionService oldService =
                serviceWithKey("old-v1", "11111111111111111111111111111111");
        var encrypted = oldService.encrypt("legacy");
        service =
                new ChatFieldEncryptionMaintenanceService(
                        messageMapper,
                        feedbackMapper,
                        failureMapper,
                        serviceWithPreviousKey(),
                        auditTrailRecorder);
        when(messageMapper.findContentRotationCandidates("active-v1", 500))
                .thenReturn(
                        List.of(row(1L, "[encrypted]", encrypted.ciphertext(), encrypted.keyId())));
        when(feedbackMapper.findCommentRotationCandidates("active-v1", 500)).thenReturn(List.of());
        when(failureMapper.findReviewCommentRotationCandidates("active-v1", 500))
                .thenReturn(List.of());

        var result = service.rotate(true, null);

        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(result.failed()).isZero();
        verify(messageMapper, never())
                .updateEncryptedContent(eq(1L), anyString(), anyString(), anyString(), eq(1));
        assertAudit("FIELD_ENCRYPTION_ROTATE", true, 500, 1, 0, 0);
    }

    @Test
    void rotateRewritesOldKeyRowsWithActiveKey() {
        FieldEncryptionService oldService =
                serviceWithKey("old-v1", "11111111111111111111111111111111");
        var encrypted = oldService.encrypt("legacy");
        service =
                new ChatFieldEncryptionMaintenanceService(
                        messageMapper,
                        feedbackMapper,
                        failureMapper,
                        serviceWithPreviousKey(),
                        auditTrailRecorder);
        when(messageMapper.findContentRotationCandidates("active-v1", 5))
                .thenReturn(
                        List.of(row(1L, "[encrypted]", encrypted.ciphertext(), encrypted.keyId())));
        when(feedbackMapper.findCommentRotationCandidates("active-v1", 5)).thenReturn(List.of());
        when(failureMapper.findReviewCommentRotationCandidates("active-v1", 5))
                .thenReturn(List.of());

        var result = service.rotate(false, 5);

        assertThat(result.updated()).isEqualTo(1);
        verify(messageMapper)
                .updateEncryptedContent(
                        eq(1L), eq("[encrypted]"), anyString(), eq("active-v1"), eq(1));
        assertAudit("FIELD_ENCRYPTION_ROTATE", false, 5, 1, 1, 0);
    }

    @Test
    void rotateCountsDecryptFailuresWithoutExposingInput() {
        when(messageMapper.findContentRotationCandidates("active-v1", 500))
                .thenReturn(List.of(row(1L, "[encrypted]", "v1:broken", "missing-v1")));
        when(feedbackMapper.findCommentRotationCandidates("active-v1", 500)).thenReturn(List.of());
        when(failureMapper.findReviewCommentRotationCandidates("active-v1", 500))
                .thenReturn(List.of());

        var result = service.rotate(false, null);

        assertThat(result.scanned()).isEqualTo(1);
        assertThat(result.updated()).isZero();
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.message()).doesNotContain("v1:broken", "missing-v1");
        Map<String, ?> detail = captureAudit("FIELD_ENCRYPTION_ROTATE");
        assertThat(detail.get("failed")).isEqualTo(1);
        assertThat(detail.toString())
                .doesNotContain("v1:broken", "missing-v1", "ciphertext", "keyId", "active-v1");
    }

    private void assertAudit(
            String action, boolean dryRun, int limit, int scanned, int updated, int failed) {
        Map<String, ?> detail = captureAudit(action);
        assertThat(detail.get("dryRun")).isEqualTo(dryRun);
        assertThat(detail.get("limit")).isEqualTo(limit);
        assertThat(detail.get("scanned")).isEqualTo(scanned);
        assertThat(detail.get("updated")).isEqualTo(updated);
        assertThat(detail.get("skipped")).isEqualTo(0);
        assertThat(detail.get("failed")).isEqualTo(failed);
        assertThat(detail.keySet())
                .containsExactlyInAnyOrder(
                        "dryRun", "limit", "scanned", "updated", "skipped", "failed");
    }

    private Map<String, ?> captureAudit(String action) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, ?>> detailCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditTrailRecorder)
                .record(eq(action), eq("field_encryption"), isNull(), detailCaptor.capture());
        return detailCaptor.getValue();
    }

    private ChatEncryptedFieldRow row(Long id, String plaintext, String ciphertext, String keyId) {
        ChatEncryptedFieldRow row = new ChatEncryptedFieldRow();
        row.setId(id);
        row.setPlaintext(plaintext);
        row.setCiphertext(ciphertext);
        row.setKeyId(keyId);
        return row;
    }

    private FieldEncryptionService serviceWithKey(String keyId, String rawKey) {
        String keyBase64 =
                Base64.getEncoder().encodeToString(rawKey.getBytes(StandardCharsets.UTF_8));
        return new FieldEncryptionService(keyBase64, keyId, "", new MockEnvironment());
    }

    private FieldEncryptionService serviceWithPreviousKey() {
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
        return new FieldEncryptionService(
                activeKey, "active-v1", "old-v1:" + previousKey, new MockEnvironment());
    }
}
