package kr.co.cleverchat.domain.search.controller;

import java.time.LocalDate;
import kr.co.cleverchat.domain.search.service.SearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/search")
public class AdmSearchController {
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private kr.co.cleverchat.domain.settings.RuntimeSettingsService runtime;

    @org.springframework.web.bind.annotation.ModelAttribute("retentionDays")
    public int retentionDays() {
        return runtime == null
                ? 90
                : runtime.current()
                        .integer(
                                kr.co.cleverchat.domain.settings.RuntimeSetting
                                        .SEARCH_RETENTION_DAYS);
    }

    private final SearchService searchService;

    public AdmSearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/logs")
    public String logs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String source,
            Model model) {
        model.addAttribute("logs", searchService.logs(query, source, 50));
        model.addAttribute("query", query);
        model.addAttribute("source", source);
        return "admmgr/search/logList";
    }

    @GetMapping("/blocks")
    public String blocks(
            @RequestParam(required = false) String piiType,
            @RequestParam(required = false) String source,
            Model model) {
        model.addAttribute("blocks", searchService.blockLogs(piiType, source, 50));
        model.addAttribute("piiType", piiType);
        model.addAttribute("source", source);
        return "admmgr/search/blockList";
    }

    @GetMapping("/popular")
    public String popular(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            Model model) {
        model.addAttribute("popularQueries", searchService.popular(from, to, 50));
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "admmgr/search/popularList";
    }
}
