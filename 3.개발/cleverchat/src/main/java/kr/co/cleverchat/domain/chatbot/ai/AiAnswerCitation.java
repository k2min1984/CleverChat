package kr.co.cleverchat.domain.chatbot.ai;

public record AiAnswerCitation(
        String sourceType,
        Long scenarioId,
        String title,
        Long crawlDocumentId,
        String url,
        String matchedField,
        String snippet) {}
