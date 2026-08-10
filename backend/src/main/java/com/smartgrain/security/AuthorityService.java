package com.smartgrain.security;

import java.util.Set;

/**
 * 权限加载服务接口
 */
public interface AuthorityService {

    /**
     * 加载用户的角色编码 + 权限码
     *
     * @param userId      用户 ID
     * @param roles       输出参数：角色编码集合（ROLE_xxx）
     * @param permissions 输出参数：权限码集合（module:resource:action）
     */
    void loadAuthorities(Long userId, Set<String> roles, Set<String> permissions);
}
