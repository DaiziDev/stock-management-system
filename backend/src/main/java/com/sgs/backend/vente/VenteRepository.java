package com.sgs.backend.vente;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VenteRepository extends JpaRepository<Vente, Long> {

    List<Vente> findByEntrepriseIdOrderByDateVenteDesc(Long entrepriseId);
}
