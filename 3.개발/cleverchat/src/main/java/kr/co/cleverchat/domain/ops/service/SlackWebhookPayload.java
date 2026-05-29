package kr.co.cleverchat.domain.ops.service;

import java.util.List;
import java.util.Map;

public record SlackWebhookPayload(String text, List<Map<String, Object>> blocks) {}
