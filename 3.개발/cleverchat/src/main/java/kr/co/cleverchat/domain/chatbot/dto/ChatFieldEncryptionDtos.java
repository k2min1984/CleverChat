package kr.co.cleverchat.domain.chatbot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public final class ChatFieldEncryptionDtos {

    private ChatFieldEncryptionDtos() {}

    public record MaintenanceRequest(Boolean dryRun, @Min(1) @Max(5000) Integer limit) {}

    public record MaintenanceResponse(
            int scanned, int updated, int skipped, int failed, String message) {}
}
