package com.smartgrain.domain.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RoleCreateRequest {

    @NotBlank(message = "角色编码不能为空")
    @Pattern(regexp = "^ROLE_[A-Z][A-Z0-9_]{2,63}$", message = "角色编码格式：ROLE_ 开头大写字母数字下划线")
    private String roleCode;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64)
    private String roleName;

    @Size(max = 255)
    private String description;
}
