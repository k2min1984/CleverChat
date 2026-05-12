package kr.co.cleverchat.domain.chatbot.mapper;

import kr.co.cleverchat.domain.chatbot.model.ChatFailure;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatFailureMapper {
    void insert(ChatFailure failure);
}
