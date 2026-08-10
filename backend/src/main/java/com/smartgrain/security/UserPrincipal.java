package com.smartgrain.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 认证主体 - 封装登录用户的核心信息
 *
 * 注意：authorities 同时包含「角色(ROLE_xxx)」和「权限码(module:resource:action)」
 *      - 角色用于 hasRole() 判定
 *      - 权限码用于 hasAuthority() / @PreAuthorize 判定
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private Integer status;
    private Set<String> roles;       // 角色编码，如 ROLE_ADMIN
    private Set<String> permissions; // 权限码，如 system:user:create

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (permissions == null && roles == null) {
            return Collections.emptyList();
        }
        List<SimpleGrantedAuthority> list = new java.util.ArrayList<>();
        if (roles != null) {
            list.addAll(roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList()));
        }
        if (permissions != null) {
            list.addAll(permissions.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList()));
        }
        return list;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != null && status == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == 1;
    }
}
