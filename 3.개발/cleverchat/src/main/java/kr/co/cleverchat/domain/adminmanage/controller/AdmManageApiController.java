package kr.co.cleverchat.domain.adminmanage.controller;

import jakarta.validation.Valid;
import java.util.Locale;
import kr.co.cleverchat.common.api.ApiResponse;
import kr.co.cleverchat.domain.adminmanage.dto.AdminManageDtos.CodeRequest;
import kr.co.cleverchat.domain.adminmanage.dto.AdminManageDtos.MenuRequest;
import kr.co.cleverchat.domain.adminmanage.dto.AdminManageDtos.PermissionRequest;
import kr.co.cleverchat.domain.adminmanage.service.AdminManageService;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CurrentUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/admin/api/manage")
public class AdmManageApiController {

    private final AdminManageService adminManageService;

    public AdmManageApiController(AdminManageService adminManageService) {
        this.adminManageService = adminManageService;
    }

    @GetMapping("/codes")
    public ApiResponse<?> codes() {
        return ApiResponse.ok(adminManageService.codes());
    }

    @PostMapping("/codes")
    public ApiResponse<?> createCode(
            @Valid @RequestBody CodeRequest request, @CurrentUser AdminSession adminSession) {
        return ApiResponse.ok(adminManageService.createCode(request, userId(adminSession)));
    }

    @PutMapping("/codes/{id}")
    public ApiResponse<?> updateCode(
            @PathVariable Long id,
            @Valid @RequestBody CodeRequest request,
            @CurrentUser AdminSession adminSession) {
        return ApiResponse.ok(adminManageService.updateCode(id, request, userId(adminSession)));
    }

    @DeleteMapping("/codes/{id}")
    public ApiResponse<?> disableCode(
            @PathVariable Long id, @CurrentUser AdminSession adminSession) {
        return ApiResponse.ok(adminManageService.disableCode(id, userId(adminSession)));
    }

    @GetMapping("/menus")
    public ApiResponse<?> menus() {
        return ApiResponse.ok(adminManageService.menus());
    }

    @PostMapping("/menus")
    public ApiResponse<?> createMenu(
            @Valid @RequestBody MenuRequest request, @CurrentUser AdminSession adminSession) {
        return ApiResponse.ok(adminManageService.createMenu(request, userId(adminSession)));
    }

    @PutMapping("/menus/{id}")
    public ApiResponse<?> updateMenu(
            @PathVariable Long id,
            @Valid @RequestBody MenuRequest request,
            @CurrentUser AdminSession adminSession) {
        return ApiResponse.ok(adminManageService.updateMenu(id, request, userId(adminSession)));
    }

    @DeleteMapping("/menus/{id}")
    public ApiResponse<?> disableMenu(
            @PathVariable Long id, @CurrentUser AdminSession adminSession) {
        return ApiResponse.ok(adminManageService.disableMenu(id, userId(adminSession)));
    }

    @GetMapping("/permissions/{roleCode}")
    public ApiResponse<?> permission(@PathVariable String roleCode) {
        return ApiResponse.ok(adminManageService.permission(normalizeRoleCode(roleCode)));
    }

    @PutMapping("/permissions/{roleCode}")
    public ApiResponse<?> updatePermission(
            @PathVariable String roleCode,
            @RequestBody PermissionRequest request,
            @CurrentUser AdminSession adminSession) {
        return ApiResponse.ok(
                adminManageService.updatePermission(
                        normalizeRoleCode(roleCode), request, userId(adminSession)));
    }

    private Long userId(AdminSession adminSession) {
        return adminSession == null ? null : adminSession.getId();
    }

    private String normalizeRoleCode(String roleCode) {
        String normalized = roleCode == null ? "" : roleCode.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }
}
