package com.smartgrain.domain.iam.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PermissionVO {
    private Long id;
    private Long parentId;
    private String permissionCode;
    private String permissionName;
    private Integer permissionType;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private List<PermissionVO> children;
}
