package kr.co.cleverchat.domain.chatbot.controller;

import jakarta.validation.Valid;
import kr.co.cleverchat.common.api.ApiResponse;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CurrentUser;
import kr.co.cleverchat.domain.chatbot.dto.ChatAdminDtos.FailureReviewRequest;
import kr.co.cleverchat.domain.chatbot.dto.ChatAdminDtos.RecommendationSaveRequest;
import kr.co.cleverchat.domain.chatbot.model.ChatRecommendation;
import kr.co.cleverchat.domain.chatbot.service.ChatAdminService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/chat")
public class AdmChatApiController {

    private final ChatAdminService chatAdminService;

    public AdmChatApiController(ChatAdminService chatAdminService) {
        this.chatAdminService = chatAdminService;
    }

    @GetMapping("/sessions")
    public ApiResponse<?> sessions(@RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(chatAdminService.sessions(limit));
    }

    @GetMapping("/sessions/{id}")
    public ApiResponse<?> sessionDetail(@PathVariable String id) {
        return ApiResponse.ok(chatAdminService.sessionDetail(id));
    }

    @GetMapping("/failures")
    public ApiResponse<?> failures(
            @RequestParam(required = false) Boolean reviewed,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(chatAdminService.failures(reviewed, limit));
    }

    @PostMapping("/failures/{id}/review")
    public ApiResponse<Void> reviewFailure(
            @PathVariable Long id,
            @Valid @RequestBody FailureReviewRequest request,
            @CurrentUser AdminSession adminSession) {
        chatAdminService.reviewFailure(
                id, adminSession == null ? null : adminSession.getId(), request.comment());
        return ApiResponse.ok();
    }

    @GetMapping("/feedback")
    public ApiResponse<?> feedback(
            @RequestParam(required = false) String rating,
            @RequestParam(required = false) Integer limit) {
        return ApiResponse.ok(chatAdminService.feedback(rating, limit));
    }

    @GetMapping("/recommendations")
    public ApiResponse<?> recommendations(@RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(chatAdminService.recommendations(enabled));
    }

    @PostMapping("/recommendations")
    public ApiResponse<ChatRecommendation> createRecommendation(
            @Valid @RequestBody RecommendationSaveRequest request) {
        return ApiResponse.ok(chatAdminService.createRecommendation(request));
    }

    @org.springframework.web.bind.annotation.PutMapping("/recommendations/{id}")
    public ApiResponse<ChatRecommendation> updateRecommendation(
            @PathVariable Long id, @Valid @RequestBody RecommendationSaveRequest request) {
        return ApiResponse.ok(chatAdminService.updateRecommendation(id, request));
    }

    @DeleteMapping("/recommendations/{id}")
    public ApiResponse<Void> deleteRecommendation(@PathVariable Long id) {
        chatAdminService.deleteRecommendation(id);
        return ApiResponse.ok();
    }
}
