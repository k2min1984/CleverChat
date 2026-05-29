package kr.co.cleverchat.domain.chatbot.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import kr.co.cleverchat.common.api.ApiResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.FeedbackRequest;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.FeedbackResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.FreeTextRequest;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.HistorySessionResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.SelectOptionRequest;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.SessionResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatRuntimeDtos.StartRequest;
import kr.co.cleverchat.domain.chatbot.service.ChatRuntimeService;
import kr.co.cleverchat.domain.chatbot.service.ChatRuntimeService.ChatRequestContext;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat/api")
public class ChatRuntimeApiController {

    private static final String ANONYMOUS_COOKIE = "anonymous_id";

    private final ChatRuntimeService chatRuntimeService;

    public ChatRuntimeApiController(ChatRuntimeService chatRuntimeService) {
        this.chatRuntimeService = chatRuntimeService;
    }

    @PostMapping("/sessions")
    public ApiResponse<SessionResponse> start(
            @Valid @RequestBody StartRequest request,
            @CookieValue(name = ANONYMOUS_COOKIE, required = false) String anonymousCookie,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        ChatRequestContext context = context(anonymousCookie, servletRequest, servletResponse);
        return ApiResponse.ok(chatRuntimeService.start(request.scenarioId(), context));
    }

    @GetMapping("/scenarios")
    public ApiResponse<?> scenarios() {
        return ApiResponse.ok(chatRuntimeService.activeScenarios());
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<SessionResponse> get(
            @PathVariable UUID sessionId,
            @CookieValue(name = ANONYMOUS_COOKIE, required = false) String anonymousCookie,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        ChatRequestContext context = context(anonymousCookie, servletRequest, servletResponse);
        return ApiResponse.ok(chatRuntimeService.get(sessionId, context));
    }

    @GetMapping("/sessions/{sessionId}/history")
    public ApiResponse<SessionResponse> history(
            @PathVariable UUID sessionId,
            @CookieValue(name = ANONYMOUS_COOKIE, required = false) String anonymousCookie,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        ChatRequestContext context = context(anonymousCookie, servletRequest, servletResponse);
        return ApiResponse.ok(chatRuntimeService.history(sessionId, context));
    }

    @GetMapping("/history")
    public ApiResponse<List<HistorySessionResponse>> historyList(
            @CookieValue(name = ANONYMOUS_COOKIE, required = false) String anonymousCookie) {
        Optional<UUID> anonymousId = parseUuid(anonymousCookie);
        if (anonymousId.isEmpty()) {
            return ApiResponse.ok(List.of());
        }
        return ApiResponse.ok(chatRuntimeService.historyList(anonymousId.get()));
    }

    @PostMapping("/sessions/{sessionId}/select-option")
    public ApiResponse<SessionResponse> selectOption(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SelectOptionRequest request,
            @CookieValue(name = ANONYMOUS_COOKIE, required = false) String anonymousCookie,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        ChatRequestContext context = context(anonymousCookie, servletRequest, servletResponse);
        return ApiResponse.ok(
                chatRuntimeService.selectOption(sessionId, request.optionId(), context));
    }

    @PostMapping("/sessions/{sessionId}/free-text")
    public ApiResponse<SessionResponse> freeText(
            @PathVariable UUID sessionId,
            @Valid @RequestBody FreeTextRequest request,
            @CookieValue(name = ANONYMOUS_COOKIE, required = false) String anonymousCookie,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        ChatRequestContext context = context(anonymousCookie, servletRequest, servletResponse);
        return ApiResponse.ok(chatRuntimeService.freeText(sessionId, request.text(), context));
    }

    @PostMapping("/messages/{messageId}/feedback")
    public ApiResponse<FeedbackResponse> feedback(
            @PathVariable Long messageId,
            @Valid @RequestBody FeedbackRequest request,
            @CookieValue(name = ANONYMOUS_COOKIE, required = false) String anonymousCookie,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        ChatRequestContext context = context(anonymousCookie, servletRequest, servletResponse);
        return ApiResponse.ok(
                chatRuntimeService.feedback(
                        messageId, request.rating(), request.comment(), context));
    }

    @GetMapping("/recommendations")
    public ApiResponse<?> recommendations() {
        return ApiResponse.ok(chatRuntimeService.recommendations());
    }

    private ChatRequestContext context(
            String anonymousCookie, HttpServletRequest request, HttpServletResponse response) {
        UUID anonymousId =
                parseUuid(anonymousCookie)
                        .orElseGet(
                                () -> {
                                    UUID generated = UUID.randomUUID();
                                    Cookie cookie =
                                            new Cookie(ANONYMOUS_COOKIE, generated.toString());
                                    cookie.setHttpOnly(true);
                                    cookie.setPath("/");
                                    cookie.setMaxAge(60 * 60 * 24 * 90);
                                    response.addCookie(cookie);
                                    return generated;
                                });
        return new ChatRequestContext(
                anonymousId, clientIp(request), request.getHeader("User-Agent"));
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return value == null || value.isBlank()
                    ? Optional.empty()
                    : Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String clientIp(HttpServletRequest request) {
        return Arrays.stream(new String[] {"X-Forwarded-For", "X-Real-IP"})
                .map(request::getHeader)
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.split(",")[0].trim())
                .findFirst()
                .orElse(request.getRemoteAddr());
    }
}
