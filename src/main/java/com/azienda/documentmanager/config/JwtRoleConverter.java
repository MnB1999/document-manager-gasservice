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
import java.util.Map;

@Component
public class JwtRoleConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    // This looks where supabase "stores" the user's role and makes it intelligible for springboot
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Map<String, Object> appMetadata = jwt.getClaim("app_metadata");

        if (appMetadata != null && appMetadata.containsKey("role")) {
            String role = (String) appMetadata.get("role");
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }
}