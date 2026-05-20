package com.azienda.documentmanager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticazione", description = "Informazioni sull'utente autenticato")
public class AuthController {

    @GetMapping("/me")
    @Operation(summary = "Restituisce le informazioni dell'utente corrente")
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