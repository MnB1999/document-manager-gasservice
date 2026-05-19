package com.azienda.documentmanager.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public UserInfoResponse getMyInfo(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Map<String, Object> appMetadata = jwtAuth.getToken().getClaim("app_metadata");
            String role = (appMetadata != null && appMetadata.containsKey("role"))
                    ? appMetadata.get("role").toString()
                    : "user";

            return new UserInfoResponse(
                    "Autenticato",
                    jwtAuth.getTokenAttributes().get("sub").toString(),
                    jwtAuth.getTokenAttributes().get("email").toString(),
                    role
            );
        }
        return new UserInfoResponse("Non autenticato correttamente", null, null, null);
    }
}