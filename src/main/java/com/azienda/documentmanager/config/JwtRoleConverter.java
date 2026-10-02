package com.azienda.documentmanager.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Set<String> KNOWN_ROLES = Set.of("ADMIN", "USER");

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> appMetadata = jwt.getClaim("app_metadata");

        if (appMetadata != null && appMetadata.get("role") instanceof String role && !role.isBlank()) {
            String normalized = role.toUpperCase(Locale.ROOT);
            if (KNOWN_ROLES.contains(normalized)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + normalized));
            }
        }
        // DENY BY DEFAULT: 403 (by security config) on accounts with no role assigned

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}