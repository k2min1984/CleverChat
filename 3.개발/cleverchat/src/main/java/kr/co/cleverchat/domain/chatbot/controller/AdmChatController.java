package kr.co.cleverchat.domain.chatbot.controller;

import kr.co.cleverchat.domain.chatbot.service.ChatAdminService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/chat")
public class AdmChatController {

    private final ChatAdminService chatAdminService;

    public AdmChatController(ChatAdminService chatAdminService) {
        this.chatAdminService = chatAdminService;
    }

    @GetMapping("/sessions")
    public String sessions(Model model) {
        model.addAttribute("sessions", chatAdminService.sessions(50));
        return "admmgr/chat/sessionList";
    }

    @GetMapping("/sessions/{id}")
    public String sessionDetail(@PathVariable String id, Model model) {
        model.addAttribute("detail", chatAdminService.sessionDetail(id));
        return "admmgr/chat/sessionDetail";
    }

    @GetMapping("/failures")
    public String failures(@RequestParam(required = false) Boolean reviewed, Model model) {
        model.addAttribute("failures", chatAdminService.failures(reviewed, 50));
        model.addAttribute("reviewed", reviewed);
        return "admmgr/chat/failureList";
    }

    @GetMapping("/feedback")
    public String feedback(@RequestParam(required = false) String rating, Model model) {
        model.addAttribute("feedbackItems", chatAdminService.feedback(rating, 50));
        model.addAttribute("rating", rating);
        return "admmgr/chat/feedbackList";
    }

    @GetMapping("/recommendations")
    public String recommendations(@RequestParam(required = false) Boolean enabled, Model model) {
        model.addAttribute("recommendations", chatAdminService.recommendations(enabled));
        model.addAttribute("enabled", enabled);
        return "admmgr/chat/recommendationList";
    }
}
