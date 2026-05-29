package kr.co.cleverchat.domain.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class ChatRuntimeDtos {

    private ChatRuntimeDtos() {}

    public record StartRequest(@NotNull @Positive Long scenarioId) {}

    public record ScenarioSummaryResponse(Long id, String title, String description) {}

    public record SelectOptionRequest(@NotNull @Positive Long optionId) {}

    public record FreeTextRequest(@NotBlank @Size(max = 500) String text) {}

    public record FeedbackRequest(
            @NotBlank @Pattern(regexp = "UP|DOWN") String rating,
            @Size(max = 1000) String comment) {}

    public record SessionResponse(
            UUID sessionId,
            Long scenarioId,
            Long versionId,
            Long currentNodeId,
            String state,
            OffsetDateTime expiresAt,
            List<MessageResponse> messages,
            List<OptionResponse> options) {}

    public record MessageResponse(
            Long id,
            int seq,
            String direction,
            Long nodeId,
            String content,
            OffsetDateTime createdAt) {}

    public record OptionResponse(Long id, String label, int sortOrder) {}

    public record RecommendationResponse(Long id, Long scenarioId, String label) {}

    public record FeedbackResponse(Long messageId, String rating, String comment) {}

    public record HistorySessionResponse(
            UUID sessionId,
            Long scenarioId,
            String scenarioTitle,
            String state,
            OffsetDateTime startedAt,
            OffsetDateTime lastActivityAt,
            int messageCount,
            String lastMessage) {}
}
