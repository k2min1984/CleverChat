package kr.co.cleverchat.domain.chatbot.ai;

import java.util.List;

public record AiAnswerSuggestionResponse(
        String answer, List<AiAnswerCitation> citations, String provider, boolean generated) {

    public static AiAnswerSuggestionResponse none(String provider) {
        return new AiAnswerSuggestionResponse(null, List.of(), provider, false);
    }
}
