package kr.co.cleverchat.domain.ops.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import kr.co.cleverchat.common.api.ApiResponse;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CurrentUser;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.NoticeRequest;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.NotificationChannelRequest;
import kr.co.cleverchat.domain.ops.dto.OpsDtos.StatisticsSummary;
import kr.co.cleverchat.domain.ops.service.OpsNotificationService;
import kr.co.cleverchat.domain.ops.service.OpsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/admin/api")
public class AdmOpsApiController {

    private final OpsService opsService;
    private final OpsNotificationService notificationService;

    public AdmOpsApiController(OpsService opsService, OpsNotificationService notificationService) {
        this.opsService = opsService;
        this.notificationService = notificationService;
    }

    @GetMapping("/statistics/summary")
    public ApiResponse<StatisticsSummary> statisticsSummary() {
        return ApiResponse.ok(opsService.statisticsSummary());
    }

    @GetMapping("/audit-logs")
    public ApiResponse<?> auditLogs(
            @RequestParam(required = false) @Size(max = 100) String actor,
            @RequestParam(required = false) @Size(max = 100) String action,
            @RequestParam(required = false) @Size(max = 100) String targetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime to,
            @RequestParam(required = false) @Min(1) @Max(200) Integer limit) {
        validateLength(actor, 100);
        validateLength(action, 100);
        validateLength(targetType, 100);
        validateRange(limit, 1, 200);
        return ApiResponse.ok(opsService.auditLogs(actor, action, targetType, from, to, limit));
    }

    @GetMapping("/notices")
    public ApiResponse<?> notices(
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) @Min(1) @Max(200) Integer limit) {
        validateRange(limit, 1, 200);
        return ApiResponse.ok(opsService.notices(enabled, limit));
    }

    @PostMapping("/notices")
    public ApiResponse<?> createNotice(
            @Valid @RequestBody NoticeRequest request, @CurrentUser AdminSession adminSession) {
        return ApiResponse.ok(opsService.createNotice(request, userId(adminSession)));
    }

    @PutMapping("/notices/{id}")
    public ApiResponse<?> updateNotice(
            @PathVariable Long id,
            @Valid @RequestBody NoticeRequest request,
            @CurrentUser AdminSession adminSession) {
        return ApiResponse.ok(opsService.updateNotice(id, request, userId(adminSession)));
    }

    @DeleteMapping("/notices/{id}")
    public ApiResponse<?> disableNotice(
            @PathVariable Long id, @CurrentUser AdminSession adminSession) {
        return ApiResponse.ok(opsService.disableNotice(id, userId(adminSession)));
    }

    @GetMapping("/notifications/channels")
    public ApiResponse<?> notificationChannels(@RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(notificationService.channels(enabled));
    }

    @PostMapping("/notifications/channels")
    public ApiResponse<?> createNotificationChannel(
            @Valid @RequestBody NotificationChannelRequest request,
            @CurrentUser AdminSession adminSession) {
        return ApiResponse.ok(notificationService.createChannel(request, userId(adminSession)));
    }

    @PutMapping("/notifications/channels/{id}")
    public ApiResponse<?> updateNotificationChannel(
            @PathVariable Long id,
            @Valid @RequestBody NotificationChannelRequest request,
            @CurrentUser AdminSession adminSession) {
        return ApiResponse.ok(notificationService.updateChannel(id, request, userId(adminSession)));
    }

    @PostMapping("/notifications/channels/{id}/test")
    public ApiResponse<?> testNotificationChannel(@PathVariable Long id) {
        return ApiResponse.ok(notificationService.testChannel(id));
    }

    @GetMapping("/notifications/events")
    public ApiResponse<?> notificationEvents(
            @RequestParam(required = false) @Size(max = 20) String status,
            @RequestParam(required = false) Boolean reviewed,
            @RequestParam(required = false) @Min(1) @Max(200) Integer limit) {
        validateLength(status, 20);
        validateRange(limit, 1, 200);
        return ApiResponse.ok(notificationService.events(status, reviewed, limit));
    }

    @PutMapping("/notifications/events/{id}/review")
    public ApiResponse<?> reviewNotificationEvent(
            @PathVariable Long id, @CurrentUser AdminSession adminSession) {
        return ApiResponse.ok(notificationService.reviewEvent(id, userId(adminSession)));
    }

    private void validateRange(Integer value, int min, int max) {
        if (value != null && (value < min || value > max)) {
            throw new IllegalArgumentException("Value is out of range.");
        }
    }

    private void validateLength(String value, int max) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException("Value is too long.");
        }
    }

    private Long userId(AdminSession adminSession) {
        return adminSession == null ? null : adminSession.getId();
    }
}
