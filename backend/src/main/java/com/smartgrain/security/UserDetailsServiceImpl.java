package com.smartgrain.security;

import com.smartgrain.common.constant.SecurityConstants;
import com.smartgrain.domain.iam.entity.SysUser;
import com.smartgrain.domain.iam.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户加载服务 - 从 DB 加载用户 + 角色 + 权限
 *
 * 注：Mapper 在 Phase 1.4 创建，此处假设 SysUserMapper 已存在
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final AuthorityService authorityService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户已禁用: " + username);
        }

        // 加载角色 + 权限
        Set<String> roles = new HashSet<>();
        Set<String> perms = new HashSet<>();
        authorityService.loadAuthorities(user.getId(), roles, perms);

        log.debug("加载用户: {} 角色: {} 权限数: {}", username, roles, perms.size());

        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .roles(roles)
                .permissions(perms)
                .build();
    }
}
