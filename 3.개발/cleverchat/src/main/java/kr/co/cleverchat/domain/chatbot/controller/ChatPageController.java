package kr.co.cleverchat.domain.chatbot.controller;

import kr.co.cleverchat.domain.ops.service.OpsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatPageController {

    private final OpsService opsService;

    public ChatPageController(OpsService opsService) {
        this.opsService = opsService;
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
}
