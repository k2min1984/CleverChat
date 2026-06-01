package kr.co.cleverchat.domain.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import kr.co.cleverchat.domain.chatbot.mapper.ChatFailureMapper;
import kr.co.cleverchat.domain.chatbot.model.ChatFailure;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChatFailureRecorder {

    private final ChatFailureMapper failureMapper;
    private final ObjectMapper objectMapper;

    public ChatFailureRecorder(ChatFailureMapper failureMapper, ObjectMapper objectMapper) {
        this.failureMapper = failureMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String sessionId, Long messageId, String reason) {
        recordFailure(sessionId, messageId, reason, Map.of());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            String sessionId, Long messageId, String reason, Map<String, ?> detail) {
        ChatFailure failure = new ChatFailure();
        failure.setSessionNo(sessionId);
        failure.setMessageNo(messageId);
        failure.setReason(reason);
        failure.setDetail(toJson(detail));
        failureMapper.insert(failure);
    }

    private String toJson(Map<String, ?> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
