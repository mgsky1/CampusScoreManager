package com.campusscore.security;

import com.campusscore.domain.Role;
import com.campusscore.domain.User;
import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security 主体。除了标准 {@link UserDetails} 字段之外，额外携带
 * 数据库主键（{@link #getId()}）与 {@link Role}，供 Controller 内快速取用。
 */
@Getter
public class AppUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String loginName;
    private final String passwordHash;
    private final Role role;

    public AppUserDetails(Long id, String loginName, String passwordHash, Role role) {
        this.id = id;
        this.loginName = loginName;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public static AppUserDetails from(User u) {
        return new AppUserDetails(u.getId(), u.getLoginName(), u.getPasswordHash(), u.getRole());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return loginName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
