package kr.co.cleverchat.domain.adminmanage.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import kr.co.cleverchat.domain.adminmanage.service.AdminManageService;
import kr.co.cleverchat.domain.auth.security.AdminSession;
import kr.co.cleverchat.domain.auth.security.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AdmManageController {

    private final AdminManageService adminManageService;

    public AdmManageController(AdminManageService adminManageService) {
        this.adminManageService = adminManageService;
    }

    @GetMapping("/admin/manage/codes")
    public String codes(Model model) {
        model.addAttribute("codes", adminManageService.codes());
        return "admmgr/manage/codeList";
    }

    @GetMapping("/admin/manage/menus")
    public String menus(Model model) {
        model.addAttribute("menus", adminManageService.menus());
        return "admmgr/manage/menuList";
    }

    @GetMapping("/admin/manage/permissions")
    public String permissions(
            @RequestParam(defaultValue = "ADMIN") String roleCode, Model model) {
        model.addAttribute("roles", adminManageService.roles());
        model.addAttribute("menus", adminManageService.menus());
        model.addAttribute("permission", adminManageService.permission(roleCode));
        return "admmgr/manage/permissionList";
    }

    @GetMapping("/admmgr/manage/codeList.do")
    public String legacyCodeList() {
        return "redirect:/admin/manage/codes";
    }

    @GetMapping("/admmgr/manage/menuList.do")
    public String legacyMenuList() {
        return "redirect:/admin/manage/menus";
    }

    @GetMapping("/admmgr/manage/authList.do")
    public String legacyAuthList() {
        return "redirect:/admin/manage/permissions";
    }

    @PostMapping(value = "/admmgr/manage/codeSelectAjax.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Object legacyCodeSelect(@RequestParam(defaultValue = "0") Long pCodeNo) {
        return adminManageService.codesByParent(pCodeNo).stream()
                .map(code -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("codeNo", code.getId());
                    row.put("pCodeNo", code.getParentId() == null ? 0L : code.getParentId());
                    row.put("codeNm", code.getName());
                    row.put("codeDepth", code.getDepth());
                    return row;
                })
                .toList();
    }

    @PostMapping(value = "/admmgr/manage/codeRegistAjaxProc.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public boolean legacyCodeCreate(
            @RequestParam(defaultValue = "0") Long pCodeNo,
            @RequestParam String codeNm,
            @RequestParam(defaultValue = "1") Integer codeDepth,
            @CurrentUser AdminSession adminSession) {
        return adminManageService.createLegacyCode(pCodeNo, codeNm, codeDepth, userId(adminSession));
    }

    @PostMapping(value = "/admmgr/manage/codeDeleteAjaxProc.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public boolean legacyCodeDelete(
            @RequestParam Long codeNo, @CurrentUser AdminSession adminSession) {
        return adminManageService.disableCode(codeNo, userId(adminSession)) != null;
    }

    @PostMapping(value = "/admmgr/manage/codeUpdateCodeNmAjaxProc.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public boolean legacyCodeNameUpdate(
            @RequestParam Long codeNo,
            @RequestParam String codeNm,
            @CurrentUser AdminSession adminSession) {
        return adminManageService.updateCodeName(codeNo, codeNm, userId(adminSession));
    }

    @PostMapping(value = "/admmgr/manage/codeUpdateSortAjaxProc.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public boolean legacyCodeSortUpdate(
            @RequestParam(name = "codeNo", required = false) List<Long> codeNos,
            @CurrentUser AdminSession adminSession) {
        return adminManageService.updateCodeSort(codeNos, userId(adminSession));
    }

    @PostMapping(value = "/admmgr/manage/codeRefreshAjaxProc.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public boolean legacyCodeRefresh() {
        return true;
    }

    @PostMapping(value = "/admmgr/manage/menuSelectAjax.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Object legacyMenuSelect(@RequestParam(defaultValue = "0") Long pMenuNo) {
        return adminManageService.menusByParent(pMenuNo).stream()
                .map(menu -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("menuNo", menu.getId());
                    row.put("pMenuNo", menu.getParentId() == null ? 0L : menu.getParentId());
                    row.put("menuNm", menu.getTitle());
                    row.put("menuDepth", menu.getDepth());
                    return row;
                })
                .toList();
    }

    @PostMapping(value = "/admmgr/manage/menuRegistAjaxProc.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public boolean legacyMenuCreate(
            @RequestParam(defaultValue = "0") Long pMenuNo,
            @RequestParam String menuNm,
            @RequestParam(defaultValue = "1") Integer menuDepth,
            @CurrentUser AdminSession adminSession) {
        return adminManageService.createLegacyMenu(pMenuNo, menuNm, menuDepth, userId(adminSession));
    }

    @PostMapping(value = "/admmgr/manage/menuDeleteAjaxProc.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public boolean legacyMenuDelete(
            @RequestParam Long menuNo, @CurrentUser AdminSession adminSession) {
        return adminManageService.disableMenu(menuNo, userId(adminSession)) != null;
    }

    @PostMapping(value = "/admmgr/manage/menuNmUpdateAjaxProc.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public boolean legacyMenuNameUpdate(
            @RequestParam Long menuNo,
            @RequestParam String menuNm,
            @CurrentUser AdminSession adminSession) {
        return adminManageService.updateMenuName(menuNo, menuNm, userId(adminSession));
    }

    @PostMapping(value = "/admmgr/manage/menuSortUpdateAjaxProc.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public boolean legacyMenuSortUpdate(
            @RequestParam(name = "menuNo", required = false) List<Long> menuNos,
            @CurrentUser AdminSession adminSession) {
        return adminManageService.updateMenuSort(menuNos, userId(adminSession));
    }

    @PostMapping(value = "/admmgr/manage/menuRefreshAjaxProc.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public boolean legacyMenuRefresh() {
        return true;
    }

    @PostMapping(value = "/admmgr/manage/authAdminSelectAjax.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Object legacyAuthSelect() {
        return adminManageService.roles().stream()
                .map(role -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("authNo", role.getId());
                    row.put("authNm", role.getCode());
                    row.put("authDc", role.getDescription());
                    return row;
                })
                .toList();
    }

    @PostMapping(value = "/admmgr/manage/authAdminRegistAjaxProc.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public boolean legacyAuthCreate(
            @RequestParam String authNm, @CurrentUser AdminSession adminSession) {
        return adminManageService.createRole(authNm, userId(adminSession));
    }

    @PostMapping(value = "/admmgr/manage/authAdminDeleteAjaxProc.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public boolean legacyAuthDelete(
            @RequestParam Long authNo, @CurrentUser AdminSession adminSession) {
        return adminManageService.disableRole(authNo, userId(adminSession));
    }

    @PostMapping(value = "/admmgr/manage/authAdminMenuSelectAjax.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public Object legacyAuthMenuSelect(@RequestParam Long authNo) {
        return adminManageService.adminAuthMenuRows(authNo);
    }

    @PostMapping(value = "/admmgr/manage/authAdminMenuRegistAjaxProc.do", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public boolean legacyAuthMenuSave(
            @RequestParam Long authNo,
            @RequestParam(name = "arrMenuNo", required = false) List<Long> menuNos,
            @RequestParam(name = "arrPMenuNo", required = false) List<Long> parentMenuNos,
            @RequestParam(name = "arrSelectYn", required = false) List<String> selectYns,
            @RequestParam(name = "arrInsertYn", required = false) List<String> insertYns,
            @RequestParam(name = "arrUpdateYn", required = false) List<String> updateYns,
            @RequestParam(name = "arrDeleteYn", required = false) List<String> deleteYns,
            @RequestParam(name = "arrProcYn", required = false) List<String> procYns) {
        return adminManageService.updateAdminAuthMenus(
                authNo, menuNos, parentMenuNos, selectYns, insertYns, updateYns, deleteYns, procYns);
    }

    @GetMapping("/admmgr/manage/codeLayer.do")
    public String legacyCodeLayer(@RequestParam Long codeNo, Model model) {
        model.addAttribute("rMap", adminManageService.codeLayer(codeNo));
        return "admmgr/manage/codeLayer";
    }

    @GetMapping("/admmgr/manage/menuLayer.do")
    public String legacyMenuLayer(@RequestParam Long menuNo, Model model) {
        model.addAttribute("rMap", adminManageService.menuLayer(menuNo));
        return "admmgr/manage/menuLayer";
    }

    @PostMapping(value = "/admmgr/manage/codeLayerRegistProc.do", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String legacyCodeLayerSave(
            @RequestParam Long codeNo,
            @RequestParam String codeNm,
            @RequestParam(required = false) String codeVal,
            @RequestParam(required = false) String codeGroup,
            @RequestParam(required = false) String codeDc,
            @CurrentUser AdminSession adminSession) {
        adminManageService.updateLegacyCodeLayer(
                codeNo, codeNm, codeVal, codeGroup, codeDc, userId(adminSession));
        return "<script>alert('저장되었습니다.');</script>";
    }

    @PostMapping(value = "/admmgr/manage/menuLayerRegistProc.do", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String legacyMenuLayerSave(
            @RequestParam Long menuNo,
            @RequestParam String menuNm,
            @RequestParam(defaultValue = "Y") String openYn,
            @RequestParam(defaultValue = "N") String targetBlankYn,
            @RequestParam(defaultValue = "ADM") String menuType,
            @RequestParam(required = false) String menuUrl,
            @CurrentUser AdminSession adminSession) {
        adminManageService.updateLegacyMenuLayer(
                menuNo, menuNm, openYn, targetBlankYn, menuType, menuUrl, userId(adminSession));
        return "<script>alert('저장되었습니다.');</script>";
    }

    private Long userId(AdminSession adminSession) {
        return adminSession == null ? null : adminSession.getId();
    }
}
