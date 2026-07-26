package com.sis.iids.auth;

import java.util.List;

public record LoginResponse(
        String token,
        String tokenType,
        String username,
        String displayName,
        List<String> roles
) {
}
