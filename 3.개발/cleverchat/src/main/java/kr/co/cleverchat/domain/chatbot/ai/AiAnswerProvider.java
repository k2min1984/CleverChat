package kr.co.cleverchat.domain.chatbot.ai;

public interface AiAnswerProvider {

    AiAnswerSuggestionResponse generate(AiAnswerPrompt prompt);
}
