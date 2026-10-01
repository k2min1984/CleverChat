package kr.co.cleverchat.domain.chatbot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import kr.co.cleverchat.domain.chatbot.mapper.ChatFailureMapper;
import kr.co.cleverchat.domain.chatbot.model.ChatFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ChatFailureRecorder {

    private static final Logger log = LoggerFactory.getLogger(ChatFailureRecorder.class);

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
        try {
            failureMapper.insert(failure);
        } catch (DataAccessException e) {
            log.warn("Failed to record chat failure: sessionId={}, reason={}", sessionId, reason);
        }
    }

    private String toJson(Map<String, ?> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
