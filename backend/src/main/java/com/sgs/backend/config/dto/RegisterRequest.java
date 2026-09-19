package com.sgs.backend.config.dto;

import com.sgs.backend.roles.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de requête pour la création d'un utilisateur par un ADMIN.
 *
 * ⚠️ PAS de champ entrepriseId : le tenant est FORCÉ à l'entreprise de
 * l'appelant (CurrentUserService) dans AuthController. Un champ venu du
 * client permettrait à un ADMIN de créer des comptes dans une AUTRE
 * entreprise — brisant l'isolation multi-tenant. Chaque admin ne crée
 * des utilisateurs que pour SON entreprise, sans exception.
 *
 * Réservé aux ADMIN (contrôle @PreAuthorize côté controller).
 * Le mot de passe sera hashé côté backend avant sauvegarde.
 */
public record RegisterRequest(
        @NotBlank(message = "Le nom est obligatoire")
        String nom,

        @NotBlank(message = "Le prénom est obligatoire")
        String prenom,

        @NotBlank(message = "Le login est obligatoire")
        String login,

        @NotBlank(message = "Le mot de passe est obligatoire")
        String motDePasse,

        String mail,

        String numTel,

        @NotNull(message = "Le rôle est obligatoire")
        UserRole role
) {}
