package kr.co.cleverchat.domain.adminmanage.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import kr.co.cleverchat.common.audit.Audited;
import kr.co.cleverchat.common.error.BusinessException;
import kr.co.cleverchat.common.error.ErrorCode;
import kr.co.cleverchat.domain.adminmanage.dto.AdminManageDtos.CodeRequest;
import kr.co.cleverchat.domain.adminmanage.dto.AdminManageDtos.MenuRequest;
import kr.co.cleverchat.domain.adminmanage.dto.AdminManageDtos.PermissionRequest;
import kr.co.cleverchat.domain.adminmanage.dto.AdminManageDtos.PermissionResponse;
import kr.co.cleverchat.domain.adminmanage.mapper.AdminManageMapper;
import kr.co.cleverchat.domain.adminmanage.model.AdminCode;
import kr.co.cleverchat.domain.adminmanage.model.AdminMenu;
import kr.co.cleverchat.domain.adminmanage.model.RoleInfo;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.RequireRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminManageService {

    private static final int DEFAULT_SORT_ORDER = 100;

    private final AdminManageMapper mapper;

    public AdminManageService(AdminManageMapper mapper) {
        this.mapper = mapper;
    }

    public List<AdminCode> codes() {
        return mapper.findCodes();
    }

    public List<AdminCode> codesByParent(Long parentId) {
        return mapper.findCodesByParent(parentId);
    }

    @Audited(action = "ADMIN_CODE_CREATE", targetType = "ADMIN_CODE")
    @RequireRole("ADMIN")
    public AdminCode createCode(CodeRequest request, Long actorId) {
        AdminCode code = toCode(null, request, actorId);
        mapper.insertCode(code);
        return mapper.findCodeById(code.getId());
    }

    @Audited(action = "ADMIN_CODE_UPDATE", targetType = "ADMIN_CODE")
    @RequireRole("ADMIN")
    public AdminCode updateCode(Long id, CodeRequest request, Long actorId) {
        AdminCode code = toCode(id, request, actorId);
        int updated = mapper.updateCode(code);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return mapper.findCodeById(id);
    }

    @Audited(action = "ADMIN_CODE_DISABLE", targetType = "ADMIN_CODE")
    @RequireRole("ADMIN")
    public AdminCode disableCode(Long id, Long actorId) {
        int updated = mapper.disableCode(id, actorId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return mapper.findCodeById(id);
    }

    @RequireRole("ADMIN")
    public boolean createLegacyCode(Long parentId, String name, Integer depth, Long actorId) {
        mapper.insertLegacyCode(normalizeParent(parentId), trimRequired(name), depth == null ? 1 : depth, actorId);
        return true;
    }

    @RequireRole("ADMIN")
    public boolean updateCodeName(Long id, String name, Long actorId) {
        return mapper.updateCodeName(id, trimRequired(name), actorId) > 0;
    }

    public Map<String, Object> codeLayer(Long id) {
        Map<String, Object> row = mapper.findCodeLayerById(id);
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return row;
    }

    @RequireRole("ADMIN")
    public boolean updateLegacyCodeLayer(
            Long id, String name, String value, String codeGroup, String description, Long actorId) {
        return mapper.updateCodeLayer(
                        id,
                        trimRequired(name),
                        blankToNull(value),
                        blankToNull(codeGroup),
                        blankToNull(description),
                        actorId)
                > 0;
    }

    @RequireRole("ADMIN")
    public boolean updateCodeSort(List<Long> ids, Long actorId) {
        if (ids == null) {
            return true;
        }
        int order = 1;
        for (Long id : ids) {
            if (id != null) {
                mapper.updateCodeSort(id, order++, actorId);
            }
        }
        return true;
    }

    public List<AdminMenu> menus() {
        return mapper.findMenus();
    }

    public List<AdminMenu> menusByParent(Long parentId) {
        return mapper.findMenusByParent(parentId);
    }

    public List<RoleInfo> roles() {
        return mapper.findRoles();
    }

    public List<AdminMenu> sidebarMenus(AdminSession adminSession) {
        if (adminSession == null) {
            return List.of();
        }
        return buildTree(mapper.findVisibleMenusForRoles(toRoleCodes(adminSession.getRoles())));
    }

    @Audited(action = "ADMIN_MENU_CREATE", targetType = "ADMIN_MENU")
    @RequireRole("ADMIN")
    public AdminMenu createMenu(MenuRequest request, Long actorId) {
        AdminMenu menu = toMenu(null, request, actorId);
        mapper.insertMenu(menu);
        return mapper.findMenuById(menu.getId());
    }

    @Audited(action = "ADMIN_MENU_UPDATE", targetType = "ADMIN_MENU")
    @RequireRole("ADMIN")
    public AdminMenu updateMenu(Long id, MenuRequest request, Long actorId) {
        AdminMenu menu = toMenu(id, request, actorId);
        int updated = mapper.updateMenu(menu);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return mapper.findMenuById(id);
    }

    @Audited(action = "ADMIN_MENU_DISABLE", targetType = "ADMIN_MENU")
    @RequireRole("ADMIN")
    public AdminMenu disableMenu(Long id, Long actorId) {
        int updated = mapper.disableMenu(id, actorId);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return mapper.findMenuById(id);
    }

    @RequireRole("ADMIN")
    public boolean createLegacyMenu(Long parentId, String title, Integer depth, Long actorId) {
        AdminMenu menu = new AdminMenu();
        menu.setParentId(normalizeParent(parentId));
        menu.setTitle(trimRequired(title));
        menu.setSortOrder(DEFAULT_SORT_ORDER);
        menu.setEnabled(true);
        menu.setVisible(true);
        menu.setCreatedBy(actorId);
        menu.setUpdatedBy(actorId);
        mapper.insertMenu(menu);
        return true;
    }

    @RequireRole("ADMIN")
    public boolean updateMenuName(Long id, String title, Long actorId) {
        return mapper.updateMenuName(id, trimRequired(title), actorId) > 0;
    }

    public Map<String, Object> menuLayer(Long id) {
        Map<String, Object> row = mapper.findMenuLayerById(id);
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return row;
    }

    @RequireRole("ADMIN")
    public boolean updateLegacyMenuLayer(
            Long id,
            String title,
            String openYn,
            String targetBlankYn,
            String menuType,
            String url,
            Long actorId) {
        String normalizedMenuType = blankToNull(menuType);
        if (normalizedMenuType == null) {
            normalizedMenuType = "ADM";
        }
        String normalizedUrl = "N/A".equalsIgnoreCase(normalizedMenuType) ? null : blankToNull(url);
        return mapper.updateMenuLayer(
                        id,
                        trimRequired(title),
                        yesNo(openYn),
                        yesNo(targetBlankYn),
                        normalizedMenuType,
                        normalizedUrl,
                        actorId)
                > 0;
    }

    @RequireRole("ADMIN")
    public boolean updateMenuSort(List<Long> ids, Long actorId) {
        if (ids == null) {
            return true;
        }
        int order = 1;
        for (Long id : ids) {
            if (id != null) {
                mapper.updateMenuSort(id, order++, actorId);
            }
        }
        return true;
    }

    public PermissionResponse permission(String roleCode) {
        String normalizedRole = normalizeRoleCode(roleCode);
        return new PermissionResponse(normalizedRole, mapper.findMenuIdsByRoleCode(normalizedRole));
    }

    @RequireRole("ADMIN")
    public boolean createRole(String roleName, Long actorId) {
        String normalizedRole = normalizeRoleCode(roleName);
        mapper.insertRole(normalizedRole, normalizedRole, actorId);
        return true;
    }

    @RequireRole("ADMIN")
    public boolean disableRole(Long authNo, Long actorId) {
        return mapper.disableRole(authNo, actorId) > 0;
    }

    public List<Map<String, Object>> adminAuthMenuRows(Long authNo) {
        return mapper.findAdminAuthMenuRows(authNo);
    }

    @Transactional
    @RequireRole("ADMIN")
    public boolean updateAdminAuthMenus(
            Long authNo,
            List<Long> menuNos,
            List<Long> parentMenuNos,
            List<String> selectYns,
            List<String> insertYns,
            List<String> updateYns,
            List<String> deleteYns,
            List<String> procYns) {
        mapper.deleteRoleMenusByAuthNo(authNo);
        if (menuNos == null || menuNos.isEmpty()) {
            return true;
        }
        for (int i = 0; i < menuNos.size(); i++) {
            Long menuNo = menuNos.get(i);
            if (menuNo == null) {
                continue;
            }
            mapper.insertRoleMenuByAuthNo(
                    authNo,
                    menuNo,
                    valueAt(parentMenuNos, i),
                    yesNoAt(selectYns, i),
                    yesNoAt(insertYns, i),
                    yesNoAt(updateYns, i),
                    yesNoAt(deleteYns, i),
                    yesNoAt(procYns, i));
        }
        return true;
    }

    @Transactional
    @Audited(action = "ADMIN_PERMISSION_UPDATE", targetType = "ADMIN_ROLE_MENU")
    @RequireRole("ADMIN")
    public PermissionResponse updatePermission(
            String roleCode, PermissionRequest request, Long actorId) {
        String normalizedRole = normalizeRoleCode(roleCode);
        mapper.deleteRoleMenus(normalizedRole);
        if (request.menuIds() != null) {
            Set<Long> menuIds = new LinkedHashSet<>(request.menuIds());
            for (Long menuId : menuIds) {
                if (menuId != null) {
                    mapper.insertRoleMenu(normalizedRole, menuId);
                }
            }
        }
        return permission(normalizedRole);
    }

    public boolean canAccessPath(AdminSession adminSession, String path) {
        if (adminSession == null || path == null || path.isBlank()) {
            return false;
        }
        AdminMenu menu = mapper.findMenuByUrl(path);
        if (menu == null) {
            return true;
        }
        if (!Boolean.TRUE.equals(menu.getEnabled()) || !Boolean.TRUE.equals(menu.getVisible())) {
            return false;
        }
        return mapper.countReadableMenuForRoles(menu.getId(), toRoleCodes(adminSession.getRoles())) > 0;
    }

    private AdminCode toCode(Long id, CodeRequest request, Long actorId) {
        AdminCode code = new AdminCode();
        code.setId(id);
        code.setParentId(request.parentId());
        code.setCode(trimRequired(request.code()).toUpperCase(Locale.ROOT));
        code.setName(trimRequired(request.name()));
        code.setValue(blankToNull(request.value()));
        code.setDescription(blankToNull(request.description()));
        code.setSortOrder(request.sortOrder() == null ? DEFAULT_SORT_ORDER : request.sortOrder());
        code.setEnabled(request.enabled() == null || request.enabled());
        code.setCreatedBy(actorId);
        code.setUpdatedBy(actorId);
        return code;
    }

    private AdminMenu toMenu(Long id, MenuRequest request, Long actorId) {
        AdminMenu menu = new AdminMenu();
        menu.setId(id);
        menu.setParentId(request.parentId());
        menu.setMenuKey(blankToNull(request.menuKey()));
        menu.setTitle(trimRequired(request.title()));
        menu.setUrl(normalizeUrl(request.url()));
        menu.setSortOrder(request.sortOrder() == null ? DEFAULT_SORT_ORDER : request.sortOrder());
        menu.setEnabled(request.enabled() == null || request.enabled());
        menu.setVisible(request.visible() == null || request.visible());
        menu.setCreatedBy(actorId);
        menu.setUpdatedBy(actorId);
        return menu;
    }

    private List<AdminMenu> buildTree(List<AdminMenu> flatMenus) {
        Map<Long, AdminMenu> byId = new LinkedHashMap<>();
        List<AdminMenu> roots = new ArrayList<>();
        for (AdminMenu menu : flatMenus) {
            menu.setChildren(new ArrayList<>());
            byId.put(menu.getId(), menu);
        }
        for (AdminMenu menu : byId.values()) {
            if (menu.getParentId() == null || !byId.containsKey(menu.getParentId())) {
                roots.add(menu);
            } else {
                byId.get(menu.getParentId()).getChildren().add(menu);
            }
        }
        return roots;
    }

    private Collection<String> toRoleCodes(Set<String> roles) {
        List<String> roleCodes = new ArrayList<>();
        if (roles == null) {
            return roleCodes;
        }
        for (String role : roles) {
            String normalized = normalizeRoleCode(role);
            if (!normalized.isBlank()) {
                roleCodes.add(normalized);
            }
        }
        return roleCodes;
    }

    private String normalizeRoleCode(String roleCode) {
        String normalized = trimRequired(roleCode).toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring("ROLE_".length()) : normalized;
    }

    private Long normalizeParent(Long parentId) {
        return parentId == null || parentId == 0 ? null : parentId;
    }

    private Long valueAt(List<Long> values, int index) {
        return values == null || index >= values.size() ? null : values.get(index);
    }

    private String yesNoAt(List<String> values, int index) {
        if (values == null || index >= values.size()) {
            return "N";
        }
        return "Y".equalsIgnoreCase(values.get(index)) ? "Y" : "N";
    }

    private String yesNo(String value) {
        return "Y".equalsIgnoreCase(value) ? "Y" : "N";
    }

    private String normalizeUrl(String url) {
        String normalized = blankToNull(url);
        if (normalized == null) {
            return null;
        }
        if (!normalized.startsWith("/admin")) {
            throw new IllegalArgumentException("Admin menu URL must start with /admin.");
        }
        return normalized;
    }

    private String trimRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required value is blank.");
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
