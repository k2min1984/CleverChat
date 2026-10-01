package kr.co.cleverchat.domain.crawl.browser;

import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdmCrawlNativeLibController {
    private final CrawlNativeLibService nativeLibService;

    public AdmCrawlNativeLibController(CrawlNativeLibService nativeLibService) {
        this.nativeLibService = nativeLibService;
    }

    @GetMapping("/admin/crawl-native-libs")
    public String nativeLibs(Model model) {
        model.addAttribute("bundles", nativeLibService.bundles(50));
        return "admmgr/crawl/nativeLibs";
    }

    @PostMapping("/admin/crawl-native-libs")
    public String upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam String bundleVersion,
            @RequestParam String checksumSha256,
            @RequestParam String signature,
            @CurrentUser AdminSession adminSession,
            RedirectAttributes redirectAttributes) {
        try {
            Long adminId = adminSession == null ? null : adminSession.getId();
            nativeLibService.upload(file, bundleVersion, checksumSha256, signature, adminId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "네이티브 라이브러리 번들이 스테이징에 업로드되고 검증되었습니다.");
        } catch (BusinessException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/crawl-native-libs";
    }

    @PostMapping("/admin/crawl-native-libs/{id}/activate")
    public String activate(
            @PathVariable Long id,
            @CurrentUser AdminSession adminSession,
            RedirectAttributes redirectAttributes) {
        try {
            Long adminId = adminSession == null ? null : adminSession.getId();
            nativeLibService.activate(id, adminId);
            redirectAttributes.addFlashAttribute(
                    "successMessage", "네이티브 라이브러리 번들이 적용되었습니다. 다음 크롤부터 유효합니다.");
        } catch (BusinessException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/crawl-native-libs";
    }
}
