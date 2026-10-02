package com.azienda.documentmanager.dtos;

public record UserInfoResponse(
        String status,
        String supabaseId,
        String email,
        String role
) {}