package kr.co.cleverchat.domain.chatbot.ai;

public class NoopAiAnswerProvider implements AiAnswerProvider {

    @Override
    public AiAnswerSuggestionResponse generate(AiAnswerPrompt prompt) {
        return AiAnswerSuggestionResponse.none("noop");
    }
}
