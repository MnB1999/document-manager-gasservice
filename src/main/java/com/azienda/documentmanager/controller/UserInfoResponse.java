package com.azienda.documentmanager.controller;

public record UserInfoResponse(
        String status,
        String supabaseId,
        String email,
        String role
) {}