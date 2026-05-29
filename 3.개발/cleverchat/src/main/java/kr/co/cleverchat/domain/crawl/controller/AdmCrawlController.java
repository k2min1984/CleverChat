package kr.co.cleverchat.domain.crawl.controller;

import kr.co.cleverchat.domain.crawl.service.CrawlService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdmCrawlController {

    private final CrawlService crawlService;

    public AdmCrawlController(CrawlService crawlService) {
        this.crawlService = crawlService;
    }

    @GetMapping("/admin/crawl-targets")
    public String targets(@RequestParam(required = false) Boolean enabled, Model model) {
        model.addAttribute("targets", crawlService.targets(enabled));
        model.addAttribute("enabled", enabled);
        return "admmgr/crawl/targetList";
    }

    @GetMapping("/admin/crawl-documents")
    public String documents(@RequestParam(required = false) Long targetId, Model model) {
        model.addAttribute("documents", crawlService.documents(targetId));
        model.addAttribute("targetId", targetId);
        return "admmgr/crawl/documentList";
    }

    @GetMapping("/admin/crawl-runs")
    public String runs(
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String failureCode,
            Model model) {
        model.addAttribute("runs", crawlService.runLogs(targetId, status, failureCode, 100));
        model.addAttribute("targetId", targetId);
        model.addAttribute("status", status);
        model.addAttribute("failureCode", failureCode);
        return "admmgr/crawl/runList";
    }
}
