package com.sis.iids.auth;

import com.sis.iids.audit.AuditService;
import com.sis.iids.security.CurrentUser;
import com.sis.iids.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(AuthenticationManager authenticationManager, JwtService jwtService, AuditService auditService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            auditService.record("LOGIN_FAILURE", "SYS_USER", request.username(), null, request.username());
            throw ex;
        }

        CurrentUser user = (CurrentUser) authentication.getPrincipal();
        String token = jwtService.generate(user);
        auditService.record("LOGIN_SUCCESS", "SYS_USER", user.getUserId().toString(), null, user.getUsername());
        return new LoginResponse(
                token,
                "Bearer",
                user.getUsername(),
                user.getDisplayName(),
                user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
        );
    }
}
