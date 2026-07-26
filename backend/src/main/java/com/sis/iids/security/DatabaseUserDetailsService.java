package com.sis.iids.security;

import com.sis.iids.auth.SysRole;
import com.sis.iids.auth.SysUser;
import com.sis.iids.auth.SysUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Comparator;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final SysUserRepository userRepository;

    public DatabaseUserDetailsService(SysUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        var authorities = user.getRoles().stream()
                .map(SysRole::getCode)
                .sorted(Comparator.naturalOrder())
                .map(code -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + code))
                .toList();
        return new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getDisplayName(),
                Boolean.TRUE.equals(user.getEnabled()),
                authorities
        );
    }
}
