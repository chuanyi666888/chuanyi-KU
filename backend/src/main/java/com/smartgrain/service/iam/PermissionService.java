package com.smartgrain.service.iam;

import com.smartgrain.domain.iam.vo.PermissionVO;

import java.util.List;

public interface PermissionService {

    /** 全量权限树（管理员用） */
    List<PermissionVO> tree();

    /** 当前用户菜单树 */
    List<PermissionVO> currentUserMenuTree();
}
