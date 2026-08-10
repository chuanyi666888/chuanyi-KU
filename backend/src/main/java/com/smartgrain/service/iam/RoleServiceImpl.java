package com.smartgrain.service.iam;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartgrain.common.api.PageResult;
import com.smartgrain.common.api.ResultCode;
import com.smartgrain.common.exception.BusinessException;
import com.smartgrain.common.util.SecurityUtils;
import com.smartgrain.domain.iam.dto.AssignPermissionsRequest;
import com.smartgrain.domain.iam.dto.RoleCreateRequest;
import com.smartgrain.domain.iam.dto.RoleUpdateRequest;
import com.smartgrain.domain.iam.entity.SysPermission;
import com.smartgrain.domain.iam.entity.SysRole;
import com.smartgrain.domain.iam.entity.SysRolePermission;
import com.smartgrain.domain.iam.mapper.SysPermissionMapper;
import com.smartgrain.domain.iam.mapper.SysRoleMapper;
import com.smartgrain.domain.iam.mapper.SysRolePermissionMapper;
import com.smartgrain.domain.iam.vo.PermissionVO;
import com.smartgrain.domain.iam.vo.RoleVO;
import com.smartgrain.security.AuthorityServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;
    private final AuthorityServiceImpl authorityService;

    @Override
    public PageResult<RoleVO> page(int page, int size, String keyword) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SysRole::getRoleCode, keyword)
                    .or().like(SysRole::getRoleName, keyword));
        }
        wrapper.orderByAsc(SysRole::getId);

        Page<SysRole> result = roleMapper.selectPage(new Page<>(page, size), wrapper);
        List<RoleVO> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return new PageResult<>(list, result.getTotal(), page, size);
    }

    @Override
    public List<RoleVO> listAll() {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getStatus, 1)
                .orderByAsc(SysRole::getId));
        return roles.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public RoleVO getDetail(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        return toVO(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(RoleCreateRequest request) {
        Long exists = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, request.getRoleCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_KEY, "角色编码已存在");
        }
        SysRole role = new SysRole();
        role.setRoleCode(request.getRoleCode());
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        role.setStatus(1);
        role.setBuiltin(0);
        roleMapper.insert(role);
        log.info("[ROLE] 新增角色 id={} code={}", role.getId(), role.getRoleCode());
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RoleUpdateRequest request) {
        SysRole role = roleMapper.selectById(request.getId());
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        if (role.getBuiltin() != null && role.getBuiltin() == 1 && request.getStatus() != null && request.getStatus() == 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "内置角色不可禁用");
        }
        role.setRoleName(request.getRoleName());
        role.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            role.setStatus(request.getStatus());
        }
        roleMapper.updateById(role);
        // 状态变更可能影响权限缓存
        authorityService.evictAllAuthorities();
        log.info("[ROLE] 更新角色 id={}", request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        if (role.getBuiltin() != null && role.getBuiltin() == 1) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "内置角色不可删除");
        }
        long userCount = roleMapper.countUsersByRoleId(id);
        if (userCount > 0) {
            throw new BusinessException(ResultCode.ROLE_IN_USE,
                    "角色仍有 " + userCount + " 个用户绑定，无法删除");
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, id));
        authorityService.evictAllAuthorities();
        log.info("[ROLE] 删除角色 id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(AssignPermissionsRequest request) {
        SysRole role = roleMapper.selectById(request.getRoleId());
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        // 全量覆盖
        rolePermissionMapper.delete(new LambdaQueryWrapper<SysRolePermission>()
                .eq(SysRolePermission::getRoleId, request.getRoleId()));
        if (!CollectionUtils.isEmpty(request.getPermissionIds())) {
            for (Long pid : request.getPermissionIds()) {
                SysRolePermission rp = new SysRolePermission();
                rp.setRoleId(request.getRoleId());
                rp.setPermissionId(pid);
                rolePermissionMapper.insert(rp);
            }
        }
        // 清除所有用户权限缓存（角色权限变更影响范围大）
        authorityService.evictAllAuthorities();
        log.info("[ROLE] 分配权限 roleId={} count={}",
                request.getRoleId(),
                request.getPermissionIds() == null ? 0 : request.getPermissionIds().size());
    }

    @Override
    public List<Long> getPermissionIds(Long roleId) {
        return permissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    @Override
    public List<PermissionVO> getCurrentUserMenuTree() {
        Long userId = SecurityUtils.getCurrentUserId();
        // 通过用户 ID 查询所有角色
        List<Long> rids = new ArrayList<>();
        roleMapper.selectRolesByUserId(userId).forEach(r -> rids.add(r.getId()));
        if (rids.isEmpty()) return new ArrayList<>();

        // 取所有该用户角色绑定的权限，并只保留菜单（permissionType=1）
        List<SysPermission> perms = permissionMapper.selectPermissionsByRoleIds(rids);
        List<SysPermission> menus = perms.stream()
                .filter(p -> p.getPermissionType() != null && p.getPermissionType() == 1)
                .collect(Collectors.toList());

        return buildTree(menus);
    }

    private List<PermissionVO> buildTree(List<SysPermission> perms) {
        Map<Long, List<SysPermission>> grouped = perms.stream()
                .collect(Collectors.groupingBy(p -> p.getParentId() == null ? 0L : p.getParentId()));
        return buildChildren(grouped, 0L);
    }

    private List<PermissionVO> buildChildren(Map<Long, List<SysPermission>> grouped, Long parentId) {
        List<SysPermission> children = grouped.get(parentId);
        if (CollectionUtils.isEmpty(children)) return new ArrayList<>();
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

    private RoleVO toVO(SysRole role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setDescription(role.getDescription());
        vo.setStatus(role.getStatus());
        vo.setBuiltin(role.getBuiltin());
        vo.setCreatedAt(role.getCreatedAt());
        vo.setUserCount(roleMapper.countUsersByRoleId(role.getId()));
        return vo;
    }
}
