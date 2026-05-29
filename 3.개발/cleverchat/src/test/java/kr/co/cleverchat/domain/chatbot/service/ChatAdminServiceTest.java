package kr.co.cleverchat.domain.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.security.FieldEncryptionService;
import kr.co.cleverchat.domain.chatbot.dto.ChatAdminDtos.RecommendationSaveRequest;
import kr.co.cleverchat.domain.chatbot.mapper.ChatFailureMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatFeedbackMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatMessageMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatRecommendationMapper;
import kr.co.cleverchat.domain.chatbot.mapper.ChatSessionMapper;
import kr.co.cleverchat.domain.chatbot.model.ChatFailureQueueItem;
import kr.co.cleverchat.domain.chatbot.model.ChatFeedbackQueueItem;
import kr.co.cleverchat.domain.chatbot.model.ChatRecommendation;
import kr.co.cleverchat.domain.chatbot.model.ChatSessionListItem;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ChatAdminServiceTest {

    private final ChatFailureMapper failureMapper =
            org.mockito.Mockito.mock(ChatFailureMapper.class);
    private final ChatFeedbackMapper feedbackMapper =
            org.mockito.Mockito.mock(ChatFeedbackMapper.class);
    private final ChatRecommendationMapper recommendationMapper =
            org.mockito.Mockito.mock(ChatRecommendationMapper.class);
    private final ChatSessionMapper sessionMapper =
            org.mockito.Mockito.mock(ChatSessionMapper.class);
    private final ChatMessageMapper messageMapper =
            org.mockito.Mockito.mock(ChatMessageMapper.class);
    private final FieldEncryptionService fieldEncryptionService =
            new FieldEncryptionService("", "test-v1", "", new MockEnvironment());
    private final ChatAdminService service =
            new ChatAdminService(
                    failureMapper,
                    feedbackMapper,
                    recommendationMapper,
                    sessionMapper,
                    messageMapper,
                    fieldEncryptionService);

    @Test
    void failuresCapsLimitAtOneHundred() {
        ChatFailureQueueItem item = new ChatFailureQueueItem();
        item.setId(1L);
        when(failureMapper.findQueue(false, 100)).thenReturn(List.of(item));

        assertThat(service.failures(false, 1000)).hasSize(1);

        verify(failureMapper).findQueue(false, 100);
    }

    @Test
    void failuresDecryptReviewComment() {
        ChatFailureQueueItem item = new ChatFailureQueueItem();
        var encrypted = fieldEncryptionService.encrypt("review comment");
        item.setReviewComment("[encrypted]");
        item.setReviewCommentCiphertext(encrypted.ciphertext());
        when(failureMapper.findQueue(false, 50)).thenReturn(List.of(item));

        var result = service.failures(false, null);

        assertThat(result.get(0).getReviewComment()).isEqualTo("review comment");
    }

    @Test
    void reviewFailureMarksReviewed() {
        when(failureMapper.markReviewed(
                        org.mockito.Mockito.eq(1L),
                        org.mockito.Mockito.eq(10L),
                        org.mockito.Mockito.eq("[encrypted]"),
                        org.mockito.Mockito.anyString(),
                        org.mockito.Mockito.eq("test-v1"),
                        org.mockito.Mockito.eq(1)))
                .thenReturn(1);

        service.reviewFailure(1L, 10L, " checked ");

        verify(failureMapper)
                .markReviewed(
                        org.mockito.Mockito.eq(1L),
                        org.mockito.Mockito.eq(10L),
                        org.mockito.Mockito.eq("[encrypted]"),
                        org.mockito.Mockito.anyString(),
                        org.mockito.Mockito.eq("test-v1"),
                        org.mockito.Mockito.eq(1));
    }

    @Test
    void reviewFailureThrowsNotFoundWhenNoRowUpdated() {
        when(failureMapper.markReviewed(404L, 10L, null, null, null, null)).thenReturn(0);

        assertThatThrownBy(() -> service.reviewFailure(404L, 10L, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void feedbackCapsLimitAtOneHundred() {
        service.feedback("DOWN", 200);

        verify(feedbackMapper).findQueue("DOWN", 100);
    }

    @Test
    void feedbackDecryptsComment() {
        ChatFeedbackQueueItem item = new ChatFeedbackQueueItem();
        var encrypted = fieldEncryptionService.encrypt("feedback comment");
        item.setComment("[encrypted]");
        item.setCommentCiphertext(encrypted.ciphertext());
        when(feedbackMapper.findQueue("UP", 50)).thenReturn(List.of(item));

        var result = service.feedback("UP", null);

        assertThat(result.get(0).getComment()).isEqualTo("feedback comment");
    }

    @Test
    void sessionsCapsLimitAtOneHundred() {
        service.sessions(200);

        verify(sessionMapper).findAdminList(100);
    }

    @Test
    void sessionsDecryptLastMessage() {
        ChatSessionListItem item = new ChatSessionListItem();
        var encrypted = fieldEncryptionService.encrypt("last message");
        item.setLastMessage("[encrypted]");
        item.setLastMessageCiphertext(encrypted.ciphertext());
        when(sessionMapper.findAdminList(50)).thenReturn(List.of(item));

        var result = service.sessions(null);

        assertThat(result.get(0).getLastMessage()).isEqualTo("last message");
    }

    @Test
    void createRecommendationUsesDefaults() {
        var request = new RecommendationSaveRequest(1L, "Question", null, null);

        ChatRecommendation created = service.createRecommendation(request);

        verify(recommendationMapper).insert(created);
        assertThat(created.getPriority()).isEqualTo(100);
        assertThat(created.isEnabled()).isTrue();
    }

    @Test
    void updateRecommendationThrowsNotFoundWhenNoRowUpdated() {
        var request = new RecommendationSaveRequest(1L, "Question", 10, true);
        when(recommendationMapper.update(org.mockito.Mockito.any(ChatRecommendation.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> service.updateRecommendation(404L, request))
                .isInstanceOf(BusinessException.class);
    }
}
