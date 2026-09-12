package com.sgs.backend.commande;

/**
 * Cycle de vie d'une commande fournisseur (roadmap #8/#9) :
 * EN_ATTENTE -> RECUE     (déclenche les entrées de stock, irréversible)
 * EN_ATTENTE -> ANNULEE   (rien n'a encore bougé)
 *
 * Une commande RECUE ne peut pas être réceptionnée une seconde fois --
 * le stock serait compté deux fois (règle explicite du flux fonctionnel).
 */
public enum StatutCommandeFournisseur {
    EN_ATTENTE,
    RECUE,
    ANNULEE
}
