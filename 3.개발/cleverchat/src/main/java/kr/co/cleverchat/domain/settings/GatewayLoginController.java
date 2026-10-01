package kr.co.cleverchat.domain.settings;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GatewayLoginController {
    private final SystemSettingsService settings;

    public GatewayLoginController(SystemSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping("/gateway-login")
    public String login(org.springframework.ui.Model model) {
        model.addAttribute("gatewayLoginUrl", settings.current().gatewayUrl());
        return "gatewayLogin";
    }
}
