package com.sgs.backend.utilisateur;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    boolean existsByLogin(String login);

    Optional<Utilisateur> findByLogin(String login);

    List<Utilisateur> findByEntrepriseId(Long entrepriseId);
}
