package com.sgs.backend.entreprise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntrepriseRepository extends JpaRepository<Entreprise, Long> {

    boolean existsByNom(String nom);

    /**
     * Suppression BULK (JPQL) — pourquoi pas deleteById() ?
     *
     * deleteById passe par em.remove() : l'entité passe en statut DELETED,
     * et Hibernate 6 la considère alors "non contenue" (contains()==false).
     * Or la session ouverte par open-in-view contient aussi l'Utilisateur de
     * la requête (chargé par CurrentUserService), qui RÉFÉRENCE l'entreprise     * supprimée. Au flush, le contrôle de cascade voit cette référence vers
     * une entité "non contenue" et la prend pour une instance transitoire non
     * sauvegardée → TransientObjectException au commit (403 brut côté HTTP).
     *
     * Le DELETE JPQL exécute le SQL directement SANS toucher au contexte de
     * persistance : aucune entité ne change de statut, les références des
     * autres entités restent valides, et la contrainte FK en base fait le
     * vrai travail de protection (DataIntegrityViolationException → 409).
     * Pré-requis : transaction active (@Transactional sur l'appelant).
     */
    @Modifying
    @Query("DELETE FROM Entreprise e WHERE e.id = :id")
    void deleteByIdBulk(@Param("id") Long id);
}
