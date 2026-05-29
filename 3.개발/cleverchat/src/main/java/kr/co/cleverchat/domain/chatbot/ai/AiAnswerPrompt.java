package kr.co.cleverchat.domain.chatbot.ai;

import java.util.List;

public record AiAnswerPrompt(
        String query, AiAnswerContext context, List<AiAnswerCitation> citations) {}
