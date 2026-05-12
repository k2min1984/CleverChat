package kr.co.cleverchat.domain.chatbot.mapper;

import java.util.List;
import kr.co.cleverchat.domain.chatbot.model.ChatRecommendation;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatRecommendationMapper {
    List<ChatRecommendation> findEnabledForActiveScenarios();
}
