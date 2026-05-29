package kr.co.cleverchat.domain.search.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import kr.co.cleverchat.common.api.ApiResponse;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CurrentUser;
import kr.co.cleverchat.domain.search.dto.SearchDtos.PopularRebuildRequest;
import kr.co.cleverchat.domain.search.dto.SearchDtos.PopularRebuildResponse;
import kr.co.cleverchat.domain.search.dto.SearchDtos.RetentionResponse;
import kr.co.cleverchat.domain.search.dto.SearchDtos.SearchResponse;
import kr.co.cleverchat.domain.search.dto.SearchDtos.SearchTestRequest;
import kr.co.cleverchat.domain.search.service.SearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/admin/api/search")
public class AdmSearchApiController {

    private final SearchService searchService;

    public AdmSearchApiController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/logs")
    public ApiResponse<?> logs(
            @RequestParam(required = false) @Size(max = 200) String query,
            @RequestParam(required = false) @Size(max = 50) String source,
            @RequestParam(required = false) @Min(1) @Max(100) Integer size) {
        validateLength(query, 200);
        validateLength(source, 50);
        validateRange(size, 1, 100);
        return ApiResponse.ok(searchService.logs(query, source, size));
    }

    @GetMapping("/blocks")
    public ApiResponse<?> blocks(
            @RequestParam(required = false) @Size(max = 50) String piiType,
            @RequestParam(required = false) @Size(max = 50) String source,
            @RequestParam(required = false) @Min(1) @Max(100) Integer size) {
        validateLength(piiType, 50);
        validateLength(source, 50);
        validateRange(size, 1, 100);
        return ApiResponse.ok(searchService.blockLogs(piiType, source, size));
    }

    @GetMapping("/popular")
    public ApiResponse<?> popular(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) @Min(1) @Max(100) Integer limit) {
        validateRange(limit, 1, 100);
        return ApiResponse.ok(searchService.popular(from, to, limit));
    }

    @PostMapping("/test")
    public ApiResponse<SearchResponse> test(
            @Valid @RequestBody SearchTestRequest request, @CurrentUser AdminSession adminSession) {
        Long userId = adminSession == null ? null : adminSession.getId();
        return ApiResponse.ok(
                searchService.search(request.query(), "ADMIN_TEST", request.limit(), userId, null));
    }

    @PostMapping("/popular/rebuild")
    public ApiResponse<PopularRebuildResponse> rebuildPopular(
            @Valid @RequestBody PopularRebuildRequest request) {
        return ApiResponse.ok(searchService.rebuildPopular(request.statDate()));
    }

    @DeleteMapping("/logs/expired")
    public ApiResponse<RetentionResponse> deleteExpired(
            @RequestParam(required = false) @Min(30) @Max(3650) Integer retentionDays,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        validateRange(retentionDays, 30, 3650);
        return ApiResponse.ok(searchService.deleteExpiredLogs(retentionDays, dryRun));
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
}
