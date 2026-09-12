package com.sgs.backend.mvtStk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MvtStkRepository extends JpaRepository<MvtStk, Long> {

    List<MvtStk> findByEntrepriseIdOrderByDateMouvementDesc(Long entrepriseId);
}
