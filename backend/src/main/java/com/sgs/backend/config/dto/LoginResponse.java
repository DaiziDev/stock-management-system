package com.sgs.backend.config.dto;

import com.sgs.backend.roles.UserRole;

/**
 * DTO de réponse retourné après une connexion réussie.
 *
 * Contient :
 * - Le token JWT à stocker côté frontend (localStorage)
 * - Les infos utilisateur (sans le mot de passe) pour l'affichage
 *
 * Le frontend stocke le token et l'envoie dans chaque requête :
 *   Authorization: Bearer <token>
 */
public record LoginResponse(
        String token,
        UserInfo user
) {
    /**
     * Informations utilisateur minimales renvoyées au frontend.
     * Pas de mot de passe, pas de données sensibles.
     */
    public record UserInfo(
            Long id,
            String nom,
            String prenom,
            String login,
            UserRole role,
            Long entrepriseId
    ) {}
}
