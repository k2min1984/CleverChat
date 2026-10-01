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
    List<ChatSessionListItem> findHistoryByUser(
            @Param("userNo") Long userNo,
            @Param("cutoff") OffsetDateTime cutoff,
            @Param("limit") int limit);

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

    int updateScenarioContext(
            @Param("id") String id,
            @Param("scenarioNo") Long scenarioNo,
            @Param("versionNo") Long versionNo,
            @Param("currentNodeNo") Long currentNodeNo,
            @Param("state") String state,
            @Param("sessionType") String sessionType,
            @Param("expiresAt") OffsetDateTime expiresAt);

    int restoreSearchSession(@Param("id") String id, @Param("expiresAt") OffsetDateTime expiresAt);

    void insertScenarioSwitchEvent(
            @Param("sessionId") String sessionId,
            @Param("fromScenarioNo") Long fromScenarioNo,
            @Param("toScenarioNo") Long toScenarioNo,
            @Param("triggerMessageNo") Long triggerMessageNo,
            @Param("detail") String detail);

    Long findLatestScenarioSwitchTriggerMessageNo(
            @Param("sessionId") String sessionId, @Param("toScenarioNo") Long toScenarioNo);

    Long findSearchBackRestoredFromMessageNo(
            @Param("sessionId") String sessionId, @Param("triggerMessageNo") Long triggerMessageNo);

    void insertNodeBackEvent(
            @Param("sessionId") String sessionId,
            @Param("fromNodeNo") Long fromNodeNo,
            @Param("toNodeNo") Long toNodeNo,
            @Param("triggerMessageNo") Long triggerMessageNo,
            @Param("detail") String detail);

    void insertSearchBackEvent(
            @Param("sessionId") String sessionId,
            @Param("fromScenarioNo") Long fromScenarioNo,
            @Param("triggerMessageNo") Long triggerMessageNo,
            @Param("detail") String detail);

    int markExpired(@Param("id") String id);

    int expireActiveBefore(@Param("cutoff") OffsetDateTime cutoff);

    int deleteCreatedBefore(@Param("cutoff") OffsetDateTime cutoff, @Param("limit") int limit);
}
