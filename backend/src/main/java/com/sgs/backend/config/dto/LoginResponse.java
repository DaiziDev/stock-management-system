package com.sgs.backend.config.dto;

import com.sgs.backend.roles.UserRole;

/**
 * DTO de réponse retourné après une connexion réussie — et après un refresh
 * réussi (même forme de réponse pour les deux opérations).
 *
 * Contient :
 * - Le token JWT d'accès (courte durée, 15 min) à stocker côté frontend
 * - Le refresh token (longue durée, 7 j) : présenté à POST /api/auth/refresh
 *   pour obtenir un nouveau couple access/refresh sans redonner ses
 *   identifiants
 * - Les infos utilisateur (sans le mot de passe) pour l'affichage
 *
 * Le frontend stocke les deux tokens et envoie le token d'accès dans chaque
 * requête : Authorization: Bearer <token>
 */
public record LoginResponse(
        String token,
        String refreshToken,
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
