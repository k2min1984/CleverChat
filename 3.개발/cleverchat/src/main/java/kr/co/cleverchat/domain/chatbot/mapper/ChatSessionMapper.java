package kr.co.cleverchat.domain.chatbot.mapper;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import kr.co.cleverchat.domain.chatbot.model.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatSessionMapper {
    Optional<ChatSession> findById(@Param("id") UUID id);
    void insert(ChatSession session);
    void lockSessionByAdvisoryKey(@Param("sessionId") UUID sessionId);
    int updateCurrentNode(
        @Param("id") UUID id,
        @Param("currentNodeId") Long currentNodeId,
        @Param("state") String state,
        @Param("expiresAt") OffsetDateTime expiresAt
    );
}
