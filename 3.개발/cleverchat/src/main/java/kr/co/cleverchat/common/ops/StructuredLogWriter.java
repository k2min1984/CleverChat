package kr.co.cleverchat.common.ops;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StructuredLogWriter {

    private static final Logger API_LOGGER = LoggerFactory.getLogger("kr.co.cleverchat.ops.api");
    private static final Logger SECURITY_LOGGER =
            LoggerFactory.getLogger("kr.co.cleverchat.ops.security");
    private static final Logger SLOW_QUERY_LOGGER =
            LoggerFactory.getLogger("kr.co.cleverchat.ops.slowquery");

    private final ObjectMapper objectMapper;

    public StructuredLogWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void apiAccess(Map<String, ?> fields) {
        API_LOGGER.info(toJson("api_access", fields));
    }

    public void securityEvent(Map<String, ?> fields) {
        SECURITY_LOGGER.info(toJson("security_event", fields));
    }

    public void slowQuery(Map<String, ?> fields) {
        SLOW_QUERY_LOGGER.warn(toJson("slow_query", fields));
    }

    public String toJson(String event, Map<String, ?> fields) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("timestamp", OffsetDateTime.now().toString());
        payload.put("event", event);
        payload.putAll(fields);
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize structured log.", e);
        }
    }
}
