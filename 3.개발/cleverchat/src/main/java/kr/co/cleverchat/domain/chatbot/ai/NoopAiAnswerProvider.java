package kr.co.cleverchat.domain.chatbot.ai;

import org.springframework.stereotype.Component;

@Component
public class NoopAiAnswerProvider implements AiAnswerProvider {

    @Override
    public AiAnswerSuggestionResponse generate(AiAnswerPrompt prompt) {
        return AiAnswerSuggestionResponse.none("noop");
    }
}
