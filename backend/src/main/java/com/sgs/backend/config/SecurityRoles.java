package com.sgs.backend.config;

/**
 * Expressions SpEL d'autorisation, centralisées ici plutôt que recopiées
 * dans chaque @PreAuthorize.
 *
 * Deux raisons à ce choix :
 *   1. La matrice des droits se lit d'un seul coup d'œil dans ce fichier,
 *      au lieu d'être dispersée sur une quinzaine de controllers.
 *   2. Une chaîne recopiée est une chaîne qu'on finit par mal recopier —
 *      et une faute de frappe dans une expression SpEL ('GESTIONAIRE' au
 *      lieu de 'GESTIONNAIRE') ne casse aucune compilation : elle ouvre
 *      ou ferme silencieusement un endpoint.
 *
 * Les constantes doivent rester des `static final String` littérales :
 * @PreAuthorize est une annotation, sa valeur doit être connue à la
 * compilation (pas d'appel de méthode, pas de concaténation dynamique).
 *
 * Correspondance avec le cahier des charges (§2) :
 *   ADMIN        — accès total (entreprises, comptes utilisateurs, paramètres)
 *   GESTIONNAIRE — commandes, fournisseurs, stock, rapports
 *   VENDEUR      — vente au comptoir + consultation articles/clients
 *
 * ADMIN est systématiquement inclus dans les expressions métier : "accès
 * total" se traduit par une présence explicite dans chaque règle, et non
 * par un contournement global de la sécurité.
 */
public final class SecurityRoles {

    /** Administration pure : entreprises, comptes utilisateurs. */
    public static final String ADMIN = "hasRole('ADMIN')";

    /**
     * Gestion métier : catalogue, commandes, fournisseurs, mouvements de
     * stock, rapports financiers. Exclut le VENDEUR, dont le périmètre se
     * limite au comptoir.
     */
    public static final String GESTION = "hasAnyRole('ADMIN','GESTIONNAIRE')";

    /**
     * Opérations de comptoir + consultation : tout utilisateur authentifié
     * de l'entreprise. Équivalent fonctionnel à `isAuthenticated()`, mais
     * énumérer les rôles rend la règle explicite et surtout la rend
     * *restrictive par défaut* : un futur rôle (ex. COMPTABLE, STAGIAIRE)
     * n'héritera pas de ces accès par simple ajout à l'énumération.
     */
    public static final String TOUS = "hasAnyRole('ADMIN','GESTIONNAIRE','VENDEUR')";

    private SecurityRoles() {
        // Classe de constantes — jamais instanciée.
    }
}
