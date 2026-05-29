package kr.co.cleverchat.domain.adminmanage.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public final class AdminManageDtos {

    private AdminManageDtos() {}

    public record CodeRequest(
            Long parentId,
            @NotBlank @Pattern(regexp = "[A-Z0-9_\\.\\-]{2,64}") String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 200) String value,
            @Size(max = 500) String description,
            @Min(0) @Max(100000) Integer sortOrder,
            Boolean enabled) {}

    public record MenuRequest(
            Long parentId,
            @Pattern(regexp = "[a-zA-Z0-9_.\\-]{2,100}") String menuKey,
            @NotBlank @Size(max = 100) String title,
            @Size(max = 300) String url,
            @Min(0) @Max(100000) Integer sortOrder,
            Boolean enabled,
            Boolean visible) {}

    public record PermissionRequest(List<Long> menuIds) {}

    public record PermissionResponse(String roleCode, List<Long> menuIds) {}
}
