package kr.co.cleverchat.domain.chatbot.mapper;

import java.util.List;
import java.util.UUID;
import kr.co.cleverchat.domain.chatbot.model.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatMessageMapper {
    List<ChatMessage> findBySessionId(@Param("sessionId") UUID sessionId);
    int selectNextSeq(@Param("sessionId") UUID sessionId);
    void insert(ChatMessage message);
}
