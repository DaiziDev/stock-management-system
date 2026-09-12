package com.sgs.backend.roles;

/**
 * Énumération des rôles utilisateur dans le système SGS.
 *
 * ADMIN       : Accès total — gère les entreprises, utilisateurs, paramètres
 * GESTIONNAIRE: Gère les commandes, fournisseurs, stock, rapports
 * VENDEUR     : Vente au comptoir + consultation articles/clients
 */
public enum UserRole {
    ADMIN,
    GESTIONNAIRE,
    VENDEUR
}
