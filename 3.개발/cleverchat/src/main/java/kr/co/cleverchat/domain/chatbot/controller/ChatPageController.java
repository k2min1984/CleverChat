package kr.co.cleverchat.domain.chatbot.controller;

import java.net.URI;
import java.net.URISyntaxException;
import kr.co.cleverchat.domain.crawl.model.CrawlDocument;
import kr.co.cleverchat.domain.crawl.service.CrawlService;
import kr.co.cleverchat.domain.ops.service.OpsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ChatPageController {

    private final OpsService opsService;
    private final CrawlService crawlService;

    public ChatPageController(OpsService opsService, CrawlService crawlService) {
        this.opsService = opsService;
        this.crawlService = crawlService;
    }

    @GetMapping("/chat")
    public String chat(Model model) {
        model.addAttribute("notices", opsService.visibleNotices());
        return "chat/chat";
    }

    @GetMapping("/chat/history")
    public String history(Model model) {
        model.addAttribute("notices", opsService.visibleNotices());
        return "chat/history";
    }

    @GetMapping("/chat/crawl-documents/{id}")
    public String crawlDocument(@PathVariable Long id, Model model) {
        CrawlDocument document = crawlService.document(id);
        model.addAttribute("document", document);
        model.addAttribute("displayContent", cleanDocumentContent(document.getContent()));
        model.addAttribute("sourceUrl", reachableSourceUrl(document.getUrl()));
        return "chat/crawlDocument";
    }

    private String cleanDocumentContent(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        StringBuilder cleaned = new StringBuilder();
        for (String rawLine : content.replace("\r\n", "\n").replace('\r', '\n').split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                appendLine(cleaned, "");
                continue;
            }
            if (isNavigationNoise(line)) {
                continue;
            }
            appendLine(cleaned, line);
        }
        String result = cleaned.toString().replaceAll("\\n{3,}", "\n\n").trim();
        return result.isBlank() ? content.trim() : result;
    }

    private void appendLine(StringBuilder target, String line) {
        if (!target.isEmpty()) {
            target.append('\n');
        }
        target.append(line);
    }

    private boolean isNavigationNoise(String line) {
        String compact = line.replaceAll("\\s+", "");
        if (compact.length() > 80) {
            return false;
        }
        if (compact.matches(".*(본문바로가기|주메뉴바로가기|하단바로가기|사이트맵|화면크기|글자크기).*")) {
            return true;
        }
        if (compact.matches(".*(로그인|로그아웃|회원가입|마이페이지|Language|English|검색어입력).*")) {
            return true;
        }
        if (compact.matches(".*(만족하셨습니까|매우만족|만족도|페이지에서제공하는정보).*")) {
            return true;
        }
        if (compact.matches(".*(담당부서|담당자|연락처|최종업데이트|페이지번호입력|다음페이지|이전페이지).*")) {
            return true;
        }
        if (compact.matches(".*(자동로그아웃|로그아웃됩니다|로그인연장|세션).*")) {
            return true;
        }
        if (compact.matches(".*(등록일|조회수|첨부파일|다운로드|미리보기|점자로보기).*")) {
            return true;
        }
        return compact.matches("(홈|Home|메뉴|검색|닫기|열기|이전|다음|이전글|다음글|TOP|맨위|전체메뉴)");
    }

    private String reachableSourceUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || !path.endsWith("/boardView.do")) {
                return url;
            }
            return new URI(
                            uri.getScheme(),
                            uri.getAuthority(),
                            path.substring(0, path.length() - "boardView.do".length())
                                    + "boardList.do",
                            null,
                            null)
                    .toString();
        } catch (URISyntaxException e) {
            return url;
        }
    }
}
