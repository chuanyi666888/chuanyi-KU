package com.smartgrain.service.iam;

import com.smartgrain.common.api.PageResult;
import com.smartgrain.domain.iam.dto.AssignPermissionsRequest;
import com.smartgrain.domain.iam.dto.RoleCreateRequest;
import com.smartgrain.domain.iam.dto.RoleUpdateRequest;
import com.smartgrain.domain.iam.vo.PermissionVO;
import com.smartgrain.domain.iam.vo.RoleVO;

import java.util.List;

public interface RoleService {

    PageResult<RoleVO> page(int page, int size, String keyword);

    List<RoleVO> listAll();

    RoleVO getDetail(Long id);

    Long create(RoleCreateRequest request);

    void update(RoleUpdateRequest request);

    void delete(Long id);

    void assignPermissions(AssignPermissionsRequest request);

    List<Long> getPermissionIds(Long roleId);

    List<PermissionVO> getCurrentUserMenuTree();
}
