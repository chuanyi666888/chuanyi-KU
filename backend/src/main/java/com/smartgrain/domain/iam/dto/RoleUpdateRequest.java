package com.smartgrain.domain.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleUpdateRequest {

    @NotNull(message = "ID 不能为空")
    private Long id;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64)
    private String roleName;

    @Size(max = 255)
    private String description;

    private Integer status;
}
