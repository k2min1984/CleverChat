package kr.co.cleverchat.domain.crawl.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import kr.co.cleverchat.common.api.ApiResponse;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CurrentUser;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.ReviewRequest;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.RunResponse;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.ScheduleRequest;
import kr.co.cleverchat.domain.crawl.dto.CrawlDtos.TargetRequest;
import kr.co.cleverchat.domain.crawl.model.CrawlTarget;
import kr.co.cleverchat.domain.crawl.service.CrawlService;
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
public class AdmCrawlApiController {

    private final CrawlService crawlService;

    public AdmCrawlApiController(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @GetMapping("/crawl-targets")
    public ApiResponse<?> targets(@RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(crawlService.targets(enabled));
    }

    @PostMapping("/crawl-targets")
    public ApiResponse<CrawlTarget> createTarget(
            @Valid @RequestBody TargetRequest request, @CurrentUser AdminSession adminSession) {
        Long adminId = adminSession == null ? null : adminSession.getId();
        return ApiResponse.ok(crawlService.createTarget(request, adminId));
    }

    @PutMapping("/crawl-targets/{id}")
    public ApiResponse<CrawlTarget> updateTarget(
            @PathVariable Long id, @Valid @RequestBody TargetRequest request) {
        return ApiResponse.ok(crawlService.updateTarget(id, request));
    }

    @PutMapping("/crawl-targets/{id}/schedule")
    public ApiResponse<CrawlTarget> updateSchedule(
            @PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.ok(crawlService.updateSchedule(id, request));
    }

    @PostMapping("/crawl-targets/schedule/preview")
    public ApiResponse<?> previewSchedule(@Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.ok(crawlService.previewSchedule(request));
    }

    @PostMapping("/crawl-targets/{id}/run")
    public ApiResponse<RunResponse> run(@PathVariable Long id) {
        return ApiResponse.ok(crawlService.run(id));
    }

    @GetMapping("/crawl-documents")
    public ApiResponse<?> documents(@RequestParam(required = false) @Positive Long targetId) {
        validatePositive(targetId);
        return ApiResponse.ok(crawlService.documents(targetId));
    }

    @GetMapping("/crawl-runs")
    public ApiResponse<?> runs(
            @RequestParam(required = false) @Positive Long targetId,
            @RequestParam(required = false)
                    @Size(max = 30)
                    @Pattern(regexp = "SUCCESS|FAILED|DUPLICATE")
                    String status,
            @RequestParam(required = false)
                    @Size(max = 40)
                    @Pattern(
                            regexp =
                                    "ROBOTS_BLOCKED|HTTP_ERROR|TIMEOUT|PARSE_ERROR|DUP_HASH|FETCH_ERROR|NOT_HTML|URL_BLOCKED|SCHEDULE_INVALID|SYSTEM_ERROR")
                    String failureCode,
            @RequestParam(required = false) @Min(1) @Max(200) Integer limit) {
        validatePositive(targetId);
        validateStatus(status);
        validateFailureCode(failureCode);
        validateRange(limit, 1, 200);
        return ApiResponse.ok(crawlService.runLogs(targetId, status, failureCode, limit));
    }

    @GetMapping("/crawl-runs/failures")
    public ApiResponse<?> failures(
            @RequestParam(required = false) Boolean reviewed,
            @RequestParam(required = false)
                    @Size(max = 40)
                    @Pattern(
                            regexp =
                                    "ROBOTS_BLOCKED|HTTP_ERROR|TIMEOUT|PARSE_ERROR|DUP_HASH|FETCH_ERROR|NOT_HTML|URL_BLOCKED|SCHEDULE_INVALID|SYSTEM_ERROR")
                    String failureCode,
            @RequestParam(required = false) @Min(1) @Max(200) Integer limit) {
        validateFailureCode(failureCode);
        validateRange(limit, 1, 200);
        return ApiResponse.ok(crawlService.failedRunLogs(reviewed, failureCode, limit));
    }

    @PutMapping("/crawl-runs/{id}/review")
    public ApiResponse<?> reviewFailure(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReviewRequest request,
            @CurrentUser AdminSession adminSession) {
        Long reviewedBy = adminSession == null ? null : adminSession.getId();
        return ApiResponse.ok(crawlService.reviewFailure(id, request, reviewedBy));
    }

    @DeleteMapping("/crawl-runs/expired")
    public ApiResponse<?> deleteExpired(
            @RequestParam(required = false) @Min(30) @Max(3650) Integer runRetentionDays,
            @RequestParam(required = false) @Min(30) @Max(3650) Integer documentRetentionDays,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        validateRange(runRetentionDays, 30, 3650);
        validateRange(documentRetentionDays, 30, 3650);
        return ApiResponse.ok(
                crawlService.deleteExpired(runRetentionDays, documentRetentionDays, dryRun));
    }

    private void validatePositive(Long value) {
        if (value != null && value < 1) {
            throw new IllegalArgumentException("Positive value is required.");
        }
    }

    private void validateRange(Integer value, int min, int max) {
        if (value != null && (value < min || value > max)) {
            throw new IllegalArgumentException("Value is out of range.");
        }
    }

    private void validateStatus(String status) {
        if (status != null && !status.matches("SUCCESS|FAILED|DUPLICATE")) {
            throw new IllegalArgumentException("Invalid crawl run status.");
        }
    }

    private void validateFailureCode(String failureCode) {
        if (failureCode != null
                && !failureCode.matches(
                        "ROBOTS_BLOCKED|HTTP_ERROR|TIMEOUT|PARSE_ERROR|DUP_HASH|FETCH_ERROR|NOT_HTML|URL_BLOCKED|SCHEDULE_INVALID|SYSTEM_ERROR")) {
            throw new IllegalArgumentException("Invalid crawl failure code.");
        }
    }
}
