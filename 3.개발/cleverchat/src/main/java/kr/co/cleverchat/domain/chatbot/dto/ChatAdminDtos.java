package kr.co.cleverchat.domain.chatbot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import kr.co.cleverchat.domain.chatbot.model.ChatFailureQueueItem;
import kr.co.cleverchat.domain.chatbot.model.ChatMessageTraceItem;
import kr.co.cleverchat.domain.chatbot.model.ChatSessionListItem;

public final class ChatAdminDtos {

    private ChatAdminDtos() {}

    public record FailureReviewRequest(@Size(max = 1000) String comment) {}

    public record RecommendationSaveRequest(
            @NotNull @Positive Long scenarioId,
            @NotBlank @Size(max = 200) String label,
            @Min(0) @Max(100000) Integer priority,
            Boolean enabled) {}

    public record SessionDetailResponse(
            ChatSessionListItem session,
            List<ChatMessageTraceItem> messages,
            List<ChatFailureQueueItem> failures) {}
}
