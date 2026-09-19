package com.sgs.backend.common;

/**
 * Levée quand une ressource viole une contrainte d'unicité MÉTIER déjà
 * vérifiée avant insertion (nom d'entreprise, login d'utilisateur...).
 *
 * 409 Conflict et non 400 : la requête est syntaxiquement valide, c'est
 * l'ÉTAT ACTUEL des données qui s'y oppose — le client peut corriger
 * (choisir un autre nom/login) et renvoyer telle quelle.
 *
 * Différence avec DataIntegrityViolationException (409 aussi) : celle-ci
 * vient de la base APRÈS l'insertion (contrainte FK, unique imprévue) ;
 * celle-là est levée volontairement par le service AVANT d'écrire, avec un
 * message clair et actionnable pour l'utilisateur du formulaire.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
