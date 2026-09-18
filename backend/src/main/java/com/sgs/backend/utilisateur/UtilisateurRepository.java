package com.sgs.backend.utilisateur;

import com.sgs.backend.roles.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

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
