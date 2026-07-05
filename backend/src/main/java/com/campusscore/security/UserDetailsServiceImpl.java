package com.campusscore.security;

import com.campusscore.persistence.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 按 {@code login_name} 大小写不敏感查询用户。DB 层已用
 * {@code LOWER(login_name)} 唯一索引保证唯一性。
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String loginName) throws UsernameNotFoundException {
        return userMapper
                .findByLoginName(loginName)
                .map(AppUserDetails::from)
                .orElseThrow(
                        () ->
                                new UsernameNotFoundException(
                                        "登录名不存在: " + loginName));
    }
}
