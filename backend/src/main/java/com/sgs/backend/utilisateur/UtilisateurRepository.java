package com.sgs.backend.utilisateur;

import com.sgs.backend.roles.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    /**
     * Suppression BULK de tous les comptes d'une entreprise — étape 1 du
     * self-destruct (DELETE /api/entreprises/me, cf. EntrepriseService.delete).
     * BULK et non deleteAll : même raison que deleteByIdBulk côté Entreprise
     * (pas de changement de statut dans le contexte de persistance). L'appelant
     * lui-même est inclus : son token meurt avec l'entreprise.
     * Pré-requis : transaction active (@Transactional sur l'appelant).
     */
    @Modifying
    @Query("DELETE FROM Utilisateur u WHERE u.entreprise.id = :entrepriseId")
    void deleteByEntrepriseIdBulk(@Param("entrepriseId") Long entrepriseId);

    boolean existsByLogin(String login);

    Optional<Utilisateur> findByLogin(String login);

    List<Utilisateur> findByEntrepriseId(Long entrepriseId);

    /**
     * Compte des utilisateurs ACTIFS d'une entreprise portant un rôle donné.
     *
     * Sert à la garde « dernier admin actif » : empêcher la désactivation     * ou la rétrogradation du seul compte encore capable d'administrer     * l'entreprise (sinon plus aucun /register, plus aucune gestion des     * comptes — verrouillage total, récupérable seulement en base).
     */
    long countByEntrepriseIdAndRoleAndActifTrue(Long entrepriseId, UserRole role);
}

