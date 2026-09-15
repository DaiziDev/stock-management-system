package com.sgs.backend.commande;

/**
 * Cycle de vie d'une commande client (roadmap #7) :
 * EN_COURS  -> VALIDEE  (déclenche les sorties de stock, irréversible)
 * EN_COURS  -> ANNULEE  (aucun impact sur le stock, rien n'a encore bougé)
 *
 * Pas de retour possible depuis VALIDEE : annuler une commande déjà validée
 * nécessiterait de générer des mouvements de stock inverses (un "avoir"),
 * ce qui est hors périmètre pour l'instant.
 */
public enum StatutCommandeClient {
    EN_COURS,
    VALIDEE,
    ANNULEE
}
