package kr.co.cleverchat.domain.chatbot.mapper;

import java.util.List;
import kr.co.cleverchat.domain.chatbot.model.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatMessageMapper {
    List<ChatMessage> findBySessionId(@Param("sessionId") String sessionId);
    int selectNextSeq(@Param("sessionId") String sessionId);
    void insert(ChatMessage message);
}
