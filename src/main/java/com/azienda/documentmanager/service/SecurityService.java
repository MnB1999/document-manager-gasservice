package com.azienda.documentmanager.service;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SecurityService {

    public boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    public UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            // On supabase "sub" is the unique user ID
            String supabaseUuid = jwtAuth.getTokenAttributes().get("sub").toString();
            return UUID.fromString(supabaseUuid);
        }

        throw new AuthenticationCredentialsNotFoundException("Utente non autenticato o token non valido");
    }

    public UUID getCurrentUserIdOrNull() {
        try {
            return getCurrentUserId();
        } catch (Exception e) {
            return null; // Null in cases like cleanup task (system triggered)
        }
    }

}
