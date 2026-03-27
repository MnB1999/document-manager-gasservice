package com.azienda.documentmanager.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public Map<String, Object> getMyInfo(Authentication auth) {
        Map<String, Object> info = new HashMap<>();

        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            info.put("status", "Autenticato");
            info.put("supabase_id", jwtAuth.getTokenAttributes().get("sub"));
            info.put("email", jwtAuth.getTokenAttributes().get("email"));
            info.put("ruoli_spring", auth.getAuthorities());
            info.put("metadata", jwtAuth.getTokenAttributes().get("app_metadata"));
        } else {
            info.put("status", "Non autenticato correttamente");
        }

        return info;
    }
}