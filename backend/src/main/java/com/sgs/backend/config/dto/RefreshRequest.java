package com.sgs.backend.config.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de requête pour POST /api/auth/refresh.
 *
 * Le frontend envoie le refresh token reçu au login (ou au refresh
 * précédent) pour obtenir un nouveau couple access/refresh.
 */
public record RefreshRequest(
        @NotBlank(message = "Le refresh token est obligatoire")
        String refreshToken
) {}
