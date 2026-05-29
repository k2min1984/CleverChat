package kr.co.cleverchat.domain.chatbot.service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import kr.co.cleverchat.common.audit.AuditTrailRecorder;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.security.FieldEncryptionService;
import kr.co.cleverchat.common.security.FieldEncryptionService.EncryptedField;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import kr.co.cleverchat.domain.chatbot.dto.ChatFieldEncryptionDtos.MaintenanceResponse;
import kr.co.cleverchat.domain.chatbot.mapper.ChatFailureMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatFeedbackMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatMessageMapper;
import kr.co.cleverchat.domain.chatbot.model.ChatEncryptedFieldRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatFieldEncryptionMaintenanceService {

    private static final int DEFAULT_LIMIT = 500;
    private static final int MAX_LIMIT = 5000;
    private static final String ENCRYPTED_PLACEHOLDER = "[encrypted]";

    private final ChatMessageMapper messageMapper;
    private final ChatFeedbackMapper feedbackMapper;
    private final ChatFailureMapper failureMapper;
    private final FieldEncryptionService fieldEncryptionService;
    private final AuditTrailRecorder auditTrailRecorder;

    public ChatFieldEncryptionMaintenanceService(
            ChatMessageMapper messageMapper,
            ChatFeedbackMapper feedbackMapper,
            ChatFailureMapper failureMapper,
            FieldEncryptionService fieldEncryptionService,
            AuditTrailRecorder auditTrailRecorder) {
        this.messageMapper = messageMapper;
        this.feedbackMapper = feedbackMapper;
        this.failureMapper = failureMapper;
        this.fieldEncryptionService = fieldEncryptionService;
        this.auditTrailRecorder = auditTrailRecorder;
    }

    @Transactional
    @RequireRole("OPERATOR")
    public MaintenanceResponse backfill(Boolean dryRun, Integer limit) {
        boolean dry = dryRun != null && dryRun;
        int resolvedLimit = resolveLimit(limit);
        Totals totals = new Totals();
        backfillRows(
                dry,
                messageMapper.findContentBackfillCandidates(resolvedLimit),
                totals,
                row -> updateMessage(row.getId(), row.getPlaintext()));
        backfillRows(
                dry,
                feedbackMapper.findCommentBackfillCandidates(resolvedLimit),
                totals,
                row -> updateFeedback(row.getId(), row.getPlaintext()));
        backfillRows(
                dry,
                failureMapper.findReviewCommentBackfillCandidates(resolvedLimit),
                totals,
                row -> updateFailure(row.getId(), row.getPlaintext()));
        MaintenanceResponse response =
                totals.response(
                        dry
                                ? "Field encryption backfill dry-run completed."
                                : "Field encryption backfill completed.");
        audit("FIELD_ENCRYPTION_BACKFILL", dry, resolvedLimit, response);
        return response;
    }

    @Transactional
    @RequireRole("OPERATOR")
    public MaintenanceResponse rotate(Boolean dryRun, Integer limit) {
        boolean dry = dryRun != null && dryRun;
        int resolvedLimit = resolveLimit(limit);
        String activeKeyId = fieldEncryptionService.activeKeyId();
        Totals totals = new Totals();
        rotateRows(
                dry,
                messageMapper.findContentRotationCandidates(activeKeyId, resolvedLimit),
                totals,
                row ->
                        fieldEncryptionService.decryptOrFallback(
                                row.getCiphertext(), row.getKeyId(), null),
                row -> updateMessage(row.getId(), row.getPlaintext()));
        rotateRows(
                dry,
                feedbackMapper.findCommentRotationCandidates(activeKeyId, resolvedLimit),
                totals,
                row ->
                        fieldEncryptionService.decryptOrFallback(
                                row.getCiphertext(), row.getKeyId(), null),
                row -> updateFeedback(row.getId(), row.getPlaintext()));
        rotateRows(
                dry,
                failureMapper.findReviewCommentRotationCandidates(activeKeyId, resolvedLimit),
                totals,
                row ->
                        fieldEncryptionService.decryptOrFallback(
                                row.getCiphertext(), row.getKeyId(), null),
                row -> updateFailure(row.getId(), row.getPlaintext()));
        MaintenanceResponse response =
                totals.response(
                        dry
                                ? "Field encryption rotation dry-run completed."
                                : "Field encryption rotation completed.");
        audit("FIELD_ENCRYPTION_ROTATE", dry, resolvedLimit, response);
        return response;
    }

    private void audit(String action, boolean dryRun, int limit, MaintenanceResponse response) {
        auditTrailRecorder.record(
                action,
                "field_encryption",
                null,
                Map.of(
                        "dryRun", dryRun,
                        "limit", limit,
                        "scanned", response.scanned(),
                        "updated", response.updated(),
                        "skipped", response.skipped(),
                        "failed", response.failed()));
    }

    private void backfillRows(
            boolean dryRun,
            List<ChatEncryptedFieldRow> rows,
            Totals totals,
            Consumer<ChatEncryptedFieldRow> updater) {
        totals.scanned += rows.size();
        if (dryRun) {
            return;
        }
        for (ChatEncryptedFieldRow row : rows) {
            try {
                updater.accept(row);
                totals.updated++;
            } catch (BusinessException e) {
                totals.failed++;
            }
        }
    }

    private void rotateRows(
            boolean dryRun,
            List<ChatEncryptedFieldRow> rows,
            Totals totals,
            Function<ChatEncryptedFieldRow, String> decryptor,
            Consumer<ChatEncryptedFieldRow> updater) {
        totals.scanned += rows.size();
        for (ChatEncryptedFieldRow row : rows) {
            try {
                row.setPlaintext(decryptor.apply(row));
                if (dryRun) {
                    continue;
                }
                updater.accept(row);
                totals.updated++;
            } catch (BusinessException e) {
                totals.failed++;
            }
        }
    }

    private void updateMessage(Long id, String plaintext) {
        EncryptedField encrypted =
                fieldEncryptionService.encrypt(plaintext == null ? "" : plaintext);
        messageMapper.updateEncryptedContent(
                id,
                ENCRYPTED_PLACEHOLDER,
                encrypted.ciphertext(),
                encrypted.keyId(),
                encrypted.version());
    }

    private void updateFeedback(Long id, String plaintext) {
        EncryptedField encrypted =
                fieldEncryptionService.encrypt(plaintext == null ? "" : plaintext);
        feedbackMapper.updateEncryptedComment(
                id,
                ENCRYPTED_PLACEHOLDER,
                encrypted.ciphertext(),
                encrypted.keyId(),
                encrypted.version());
    }

    private void updateFailure(Long id, String plaintext) {
        EncryptedField encrypted =
                fieldEncryptionService.encrypt(plaintext == null ? "" : plaintext);
        failureMapper.updateEncryptedReviewComment(
                id,
                ENCRYPTED_PLACEHOLDER,
                encrypted.ciphertext(),
                encrypted.keyId(),
                encrypted.version());
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_LIMIT));
    }

    private static class Totals {
        int scanned;
        int updated;
        int skipped;
        int failed;

        MaintenanceResponse response(String message) {
            return new MaintenanceResponse(scanned, updated, skipped, failed, message);
        }
    }
}
