package com.smartgrain.service.iam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartgrain.domain.iam.entity.SysPermission;
import com.smartgrain.domain.iam.mapper.SysPermissionMapper;
import com.smartgrain.domain.iam.vo.PermissionVO;
import com.smartgrain.security.UserPrincipal;
import com.smartgrain.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SysPermissionMapper permissionMapper;
    private final RoleService roleService;

    @Override
    public List<PermissionVO> tree() {
        List<SysPermission> all = permissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .eq(SysPermission::getStatus, 1)
                .orderByAsc(SysPermission::getSortOrder));
        return buildTree(all);
    }

    @Override
    public List<PermissionVO> currentUserMenuTree() {
        return roleService.getCurrentUserMenuTree();
    }

    private List<PermissionVO> buildTree(List<SysPermission> perms) {
        Map<Long, List<SysPermission>> grouped = perms.stream()
                .collect(Collectors.groupingBy(p -> p.getParentId() == null ? 0L : p.getParentId()));
        return buildChildren(grouped, 0L);
    }

    private List<PermissionVO> buildChildren(Map<Long, List<SysPermission>> grouped, Long parentId) {
        List<SysPermission> children = grouped.get(parentId);
        if (children == null || children.isEmpty()) return List.of();
        return children.stream()
                .sorted(Comparator.comparingInt(p -> p.getSortOrder() == null ? 0 : p.getSortOrder()))
                .map(p -> {
                    PermissionVO vo = new PermissionVO();
                    vo.setId(p.getId());
                    vo.setParentId(p.getParentId());
                    vo.setPermissionCode(p.getPermissionCode());
                    vo.setPermissionName(p.getPermissionName());
                    vo.setPermissionType(p.getPermissionType());
                    vo.setPath(p.getPath());
                    vo.setComponent(p.getComponent());
                    vo.setIcon(p.getIcon());
                    vo.setSortOrder(p.getSortOrder());
                    vo.setStatus(p.getStatus());
                    vo.setChildren(buildChildren(grouped, p.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
