package kr.co.cleverchat.domain.chatbot.mapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import kr.co.cleverchat.domain.chatbot.model.ChatSession;
import kr.co.cleverchat.domain.chatbot.model.ChatSessionListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatSessionMapper {
    Optional<ChatSession> findById(@Param("id") String id);

    List<ChatSessionListItem> findHistoryByAnonymousId(
            @Param("anonymousId") String anonymousId,
            @Param("cutoff") OffsetDateTime cutoff,
            @Param("limit") int limit);

    List<ChatSessionListItem> findAdminList(@Param("limit") int limit);

    ChatSessionListItem findAdminDetail(@Param("id") String id);

    void insert(ChatSession session);

    void lockSessionByAdvisoryKey(@Param("sessionId") String sessionId);

    int updateCurrentNode(
            @Param("id") String id,
            @Param("currentNodeId") Long currentNodeId,
            @Param("state") String state,
            @Param("expiresAt") OffsetDateTime expiresAt);

    int markExpired(@Param("id") String id);

    int expireActiveBefore(@Param("cutoff") OffsetDateTime cutoff);

    int deleteCreatedBefore(@Param("cutoff") OffsetDateTime cutoff, @Param("limit") int limit);
}
