package kr.co.cleverchat.domain.chatbot.mapper;

import java.util.List;
import kr.co.cleverchat.domain.chatbot.model.ChatEncryptedFieldRow;
import kr.co.cleverchat.domain.chatbot.model.ChatFailure;
import kr.co.cleverchat.domain.chatbot.model.ChatFailureQueueItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatFailureMapper {
    void insert(ChatFailure failure);

    List<ChatFailureQueueItem> findQueue(
            @Param("reviewed") Boolean reviewed, @Param("limit") int limit);

    List<ChatFailureQueueItem> findBySessionId(@Param("sessionId") String sessionId);

    int markReviewed(
            @Param("id") Long id,
            @Param("reviewedBy") Long reviewedBy,
            @Param("reviewComment") String reviewComment,
            @Param("reviewCommentCiphertext") String reviewCommentCiphertext,
            @Param("reviewCommentKeyId") String reviewCommentKeyId,
            @Param("reviewCommentEncryptionVersion") Integer reviewCommentEncryptionVersion);

    List<ChatEncryptedFieldRow> findReviewCommentBackfillCandidates(@Param("limit") int limit);

    List<ChatEncryptedFieldRow> findReviewCommentRotationCandidates(
            @Param("activeKeyId") String activeKeyId, @Param("limit") int limit);

    int updateEncryptedReviewComment(
            @Param("id") Long id,
            @Param("reviewComment") String reviewComment,
            @Param("reviewCommentCiphertext") String reviewCommentCiphertext,
            @Param("reviewCommentKeyId") String reviewCommentKeyId,
            @Param("reviewCommentEncryptionVersion") Integer reviewCommentEncryptionVersion);
}
