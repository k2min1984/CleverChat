package kr.co.cleverchat.domain.adminmanage.mapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import kr.co.cleverchat.domain.adminmanage.model.AdminCode;
import kr.co.cleverchat.domain.adminmanage.model.AdminMenu;
import kr.co.cleverchat.domain.adminmanage.model.RoleInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminManageMapper {

    List<AdminCode> findCodes();

    List<AdminCode> findCodesByParent(@Param("parentId") Long parentId);

    List<Map<String, Object>> findActiveCodeOptions();

    AdminCode findCodeById(@Param("id") Long id);

    Map<String, Object> findCodeLayerById(@Param("id") Long id);

    int insertCode(AdminCode code);

    int insertLegacyCode(
            @Param("parentId") Long parentId,
            @Param("name") String name,
            @Param("depth") Integer depth,
            @Param("updatedBy") Long updatedBy);

    int updateCode(AdminCode code);

    int updateCodeLayer(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("value") String value,
            @Param("codeGroup") String codeGroup,
            @Param("description") String description,
            @Param("updatedBy") Long updatedBy);

    int updateCodeName(
            @Param("id") Long id, @Param("name") String name, @Param("updatedBy") Long updatedBy);

    int updateCodeSort(
            @Param("id") Long id,
            @Param("sortOrder") Integer sortOrder,
            @Param("updatedBy") Long updatedBy);

    int disableCode(@Param("id") Long id, @Param("updatedBy") Long updatedBy);

    List<AdminMenu> findMenus();

    List<AdminMenu> findMenusByParent(@Param("parentId") Long parentId);

    List<AdminMenu> findVisibleMenusForRoles(@Param("roleCodes") Collection<String> roleCodes);

    AdminMenu findMenuById(@Param("id") Long id);

    Map<String, Object> findMenuLayerById(@Param("id") Long id);

    AdminMenu findMenuByUrl(@Param("url") String url);

    int insertMenu(AdminMenu menu);

    int updateMenu(AdminMenu menu);

    int updateMenuLayer(
            @Param("id") Long id,
            @Param("title") String title,
            @Param("openYn") String openYn,
            @Param("targetBlankYn") String targetBlankYn,
            @Param("menuType") String menuType,
            @Param("url") String url,
            @Param("updatedBy") Long updatedBy);

    int updateMenuName(
            @Param("id") Long id, @Param("title") String title, @Param("updatedBy") Long updatedBy);

    int updateMenuSort(
            @Param("id") Long id,
            @Param("sortOrder") Integer sortOrder,
            @Param("updatedBy") Long updatedBy);

    int disableMenu(@Param("id") Long id, @Param("updatedBy") Long updatedBy);

    List<RoleInfo> findRoles();

    int insertRole(
            @Param("code") String code,
            @Param("description") String description,
            @Param("updatedBy") Long updatedBy);

    int disableRole(@Param("authNo") Long authNo, @Param("updatedBy") Long updatedBy);

    List<Long> findMenuIdsByRoleCode(@Param("roleCode") String roleCode);

    List<Map<String, Object>> findAdminAuthMenuRows(@Param("authNo") Long authNo);

    int deleteRoleMenus(@Param("roleCode") String roleCode);

    int deleteRoleMenusByAuthNo(@Param("authNo") Long authNo);

    int insertRoleMenu(@Param("roleCode") String roleCode, @Param("menuId") Long menuId);

    int insertRoleMenuByAuthNo(
            @Param("authNo") Long authNo,
            @Param("menuNo") Long menuNo,
            @Param("pMenuNo") Long pMenuNo,
            @Param("selectYn") String selectYn,
            @Param("insertYn") String insertYn,
            @Param("updateYn") String updateYn,
            @Param("deleteYn") String deleteYn,
            @Param("procYn") String procYn);

    int countReadableMenuForRoles(
            @Param("menuId") Long menuId, @Param("roleCodes") Collection<String> roleCodes);
}
