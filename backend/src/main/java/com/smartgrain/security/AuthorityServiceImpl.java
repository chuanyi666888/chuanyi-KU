package com.smartgrain.security;

import com.smartgrain.common.constant.CacheKey;
import com.smartgrain.domain.iam.mapper.SysRoleMapper;
import com.smartgrain.domain.iam.mapper.SysPermissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限加载服务实现
 *
 * - 角色：从 sys_role 直接读取 role_code（已带 ROLE_ 前缀）
 * - 权限：从 sys_permission 读取 permission_code
 * - 缓存：5 分钟，用户/角色变更时主动失效
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorityServiceImpl implements AuthorityService {

    private final SysRoleMapper roleMapper;
    private final SysPermissionMapper permissionMapper;
    private final StringRedisTemplate redis;

    @Override
    public void loadAuthorities(Long userId, Set<String> roles, Set<String> permissions) {
        // 先走缓存
        String rolesKey = CacheKey.USER_ROLES + userId;
        String permsKey = CacheKey.USER_PERMS + userId;
        List<String> cachedRoles = redis.opsForList().range(rolesKey, 0, -1);
        List<String> cachedPerms = redis.opsForList().range(permsKey, 0, -1);

        if (cachedRoles != null && !cachedRoles.isEmpty()) {
            roles.addAll(cachedRoles);
        } else {
            List<String> dbRoles = roleMapper.selectRoleCodesByUserId(userId);
            if (dbRoles != null && !dbRoles.isEmpty()) {
                roles.addAll(dbRoles);
                redis.opsForList().rightPushAll(rolesKey, dbRoles);
                redis.expire(rolesKey, Duration.ofSeconds(CacheKey.TTL_MEDIUM));
            }
        }

        if (cachedPerms != null && !cachedPerms.isEmpty()) {
            permissions.addAll(cachedPerms);
        } else {
            List<String> dbPerms = permissionMapper.selectPermissionCodesByUserId(userId);
            if (dbPerms != null && !dbPerms.isEmpty()) {
                permissions.addAll(dbPerms);
                redis.opsForList().rightPushAll(permsKey, dbPerms);
                redis.expire(permsKey, Duration.ofSeconds(CacheKey.TTL_MEDIUM));
            }
        }
    }

    /** 用户/角色变更时清除缓存 */
    public void evictUserAuthorities(Long userId) {
        redis.delete(CacheKey.USER_ROLES + userId);
        redis.delete(CacheKey.USER_PERMS + userId);
        redis.delete(CacheKey.USER_INFO + userId);
    }

    /** 清除所有用户权限缓存（角色权限变更时） */
    public void evictAllAuthorities() {
        Set<String> roleKeys = redis.keys(CacheKey.USER_ROLES + "*");
        Set<String> permKeys = redis.keys(CacheKey.USER_PERMS + "*");
        Set<String> infoKeys = redis.keys(CacheKey.USER_INFO + "*");
        Set<String> all = new HashSet<>();
        if (roleKeys != null) all.addAll(roleKeys);
        if (permKeys != null) all.addAll(permKeys);
        if (infoKeys != null) all.addAll(infoKeys);
        if (!all.isEmpty()) {
            redis.delete(all);
        }
    }
}
