package kr.co.cleverchat.domain.ops.controller;

import java.time.OffsetDateTime;
import kr.co.cleverchat.domain.ops.service.OpsNotificationService;
import kr.co.cleverchat.domain.ops.service.OpsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdmOpsController {

    private final OpsService opsService;
    private final OpsNotificationService notificationService;

    public AdmOpsController(OpsService opsService, OpsNotificationService notificationService) {
        this.opsService = opsService;
        this.notificationService = notificationService;
    }

    @GetMapping("/admin/statistics")
    public String statistics(Model model) {
        model.addAttribute("summary", opsService.statisticsSummary());
        return "admmgr/ops/statistics";
    }

    @GetMapping("/admin/audit-logs")
    public String auditLogs(
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    OffsetDateTime to,
            Model model) {
        model.addAttribute("logs", opsService.auditLogs(actor, action, targetType, from, to, 50));
        model.addAttribute("actor", actor);
        model.addAttribute("action", action);
        model.addAttribute("targetType", targetType);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        return "admmgr/ops/auditLogList";
    }

    @GetMapping("/admin/notices")
    public String notices(Model model) {
        model.addAttribute("notices", opsService.notices(null, 100));
        return "admmgr/ops/noticeList";
    }

    @GetMapping("/admin/notifications")
    public String notifications(Model model) {
        model.addAttribute("channels", notificationService.channels(null));
        model.addAttribute("events", notificationService.events(null, null, 100));
        return "admmgr/ops/notificationList";
    }
}
