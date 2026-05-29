package kr.co.cleverchat.domain.chatbot.controller;

import jakarta.validation.Valid;
import kr.co.cleverchat.common.api.ApiResponse;
import kr.co.cleverchat.domain.chatbot.dto.ChatFieldEncryptionDtos.MaintenanceRequest;
import kr.co.cleverchat.domain.chatbot.dto.ChatFieldEncryptionDtos.MaintenanceResponse;
import kr.co.cleverchat.domain.chatbot.service.ChatFieldEncryptionMaintenanceService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/admin/api/security/field-encryption")
public class AdmChatSecurityApiController {

    private final ChatFieldEncryptionMaintenanceService maintenanceService;

    public AdmChatSecurityApiController(ChatFieldEncryptionMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @PostMapping("/backfill")
    public ApiResponse<MaintenanceResponse> backfill(
            @Valid @RequestBody MaintenanceRequest request) {
        return ApiResponse.ok(maintenanceService.backfill(request.dryRun(), request.limit()));
    }

    @PostMapping("/rotate")
    public ApiResponse<MaintenanceResponse> rotate(@Valid @RequestBody MaintenanceRequest request) {
        return ApiResponse.ok(maintenanceService.rotate(request.dryRun(), request.limit()));
    }
}
