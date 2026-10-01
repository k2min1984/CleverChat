package kr.co.cleverchat.domain.crawl.browser;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import kr.co.cleverchat.common.api.ApiResponse;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CurrentUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/admin/api")
public class AdmCrawlNativeLibApiController {
    private final CrawlNativeLibService nativeLibService;

    public AdmCrawlNativeLibApiController(CrawlNativeLibService nativeLibService) {
        this.nativeLibService = nativeLibService;
    }

    @GetMapping("/crawl-native-libs")
    public ApiResponse<?> bundles(@RequestParam(required = false) @Min(1) @Max(100) Integer limit) {
        return ApiResponse.ok(nativeLibService.bundles(limit));
    }

    @PostMapping("/crawl-native-libs")
    public ApiResponse<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String bundleVersion,
            @RequestParam String checksumSha256,
            @RequestParam String signature,
            @CurrentUser AdminSession adminSession) {
        Long adminId = adminSession == null ? null : adminSession.getId();
        return ApiResponse.ok(
                nativeLibService.upload(file, bundleVersion, checksumSha256, signature, adminId));
    }

    @PostMapping("/crawl-native-libs/{id}/activate")
    public ApiResponse<?> activate(@PathVariable Long id, @CurrentUser AdminSession adminSession) {
        Long adminId = adminSession == null ? null : adminSession.getId();
        return ApiResponse.ok(nativeLibService.activate(id, adminId));
    }
}
