package com.smartgrain.domain.iam.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AssignPermissionsRequest {

    @NotNull(message = "角色 ID 不能为空")
    private Long roleId;

    /** 权限 ID 列表（全量覆盖） */
    private List<Long> permissionIds;
}
