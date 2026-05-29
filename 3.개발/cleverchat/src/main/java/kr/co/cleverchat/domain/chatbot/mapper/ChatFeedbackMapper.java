package kr.co.cleverchat.domain.chatbot.mapper;

import java.util.List;
import kr.co.cleverchat.domain.chatbot.model.ChatEncryptedFieldRow;
import kr.co.cleverchat.domain.chatbot.model.ChatFeedback;
import kr.co.cleverchat.domain.chatbot.model.ChatFeedbackQueueItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatFeedbackMapper {
    ChatFeedback findByMessageId(@Param("messageId") Long messageId);

    void upsert(ChatFeedback feedback);

    List<ChatFeedbackQueueItem> findQueue(
            @Param("rating") String rating, @Param("limit") int limit);

    List<ChatEncryptedFieldRow> findCommentBackfillCandidates(@Param("limit") int limit);

    List<ChatEncryptedFieldRow> findCommentRotationCandidates(
            @Param("activeKeyId") String activeKeyId, @Param("limit") int limit);

    int updateEncryptedComment(
            @Param("id") Long id,
            @Param("comment") String comment,
            @Param("commentCiphertext") String commentCiphertext,
            @Param("commentKeyId") String commentKeyId,
            @Param("commentEncryptionVersion") Integer commentEncryptionVersion);
}
