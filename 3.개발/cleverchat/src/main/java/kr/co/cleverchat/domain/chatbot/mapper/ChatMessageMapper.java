package kr.co.cleverchat.domain.chatbot.mapper;

import java.util.List;
import java.util.Optional;
import kr.co.cleverchat.domain.chatbot.model.ChatEncryptedFieldRow;
import kr.co.cleverchat.domain.chatbot.model.ChatMessage;
import kr.co.cleverchat.domain.chatbot.model.ChatMessageTraceItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatMessageMapper {
    List<ChatMessage> findBySessionId(@Param("sessionId") String sessionId);

    List<ChatMessageTraceItem> findTraceBySessionId(@Param("sessionId") String sessionId);

    Optional<ChatMessage> findById(@Param("id") Long id);

    int selectNextSeq(@Param("sessionId") String sessionId);

    void insert(ChatMessage message);

    List<ChatEncryptedFieldRow> findContentBackfillCandidates(@Param("limit") int limit);

    List<ChatEncryptedFieldRow> findContentRotationCandidates(
            @Param("activeKeyId") String activeKeyId, @Param("limit") int limit);

    int updateEncryptedContent(
            @Param("id") Long id,
            @Param("content") String content,
            @Param("contentCiphertext") String contentCiphertext,
            @Param("contentKeyId") String contentKeyId,
            @Param("contentEncryptionVersion") Integer contentEncryptionVersion);
}
