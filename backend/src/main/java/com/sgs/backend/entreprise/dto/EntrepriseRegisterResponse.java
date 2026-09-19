package com.sgs.backend.entreprise.dto;

/**
 * Réponse de l'enregistrement public d'une entreprise.
 *
 * Volontairement MINIMALE : le login de l'admin créé (pour confirmation à
 * l'écran), mais jamais le mot de passe et aucun token — le flow produit
 * veut que l'admin aille SE CONNECTER sur l'écran de login classique avec
 * ses identifiants, comme n'importe quel autre utilisateur.
 */
public record EntrepriseRegisterResponse(
        Long entrepriseId,
        String nom,
        String adminLogin
) {}
