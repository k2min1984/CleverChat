package kr.co.cleverchat.domain.chatbot.mapper;

import java.util.List;
import kr.co.cleverchat.domain.chatbot.model.ChatRecommendation;
import kr.co.cleverchat.domain.chatbot.model.ChatRecommendationAdminItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ChatRecommendationMapper {
    List<ChatRecommendation> findEnabledForActiveScenarios();

    List<ChatRecommendationAdminItem> findAdminList(@Param("enabled") Boolean enabled);

    ChatRecommendation findById(@Param("id") Long id);

    void insert(ChatRecommendation recommendation);

    int update(ChatRecommendation recommendation);

    int delete(@Param("id") Long id);
}
