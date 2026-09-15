package com.sgs.backend.commandeFournisseur.dto;

import java.math.BigDecimal;

public record LigneCommandeFournisseurResponseDTO(
        Long id,
        Long articleId,
        String articleDesignation,
        int quantite,
        BigDecimal prixUnitaire,
        BigDecimal sousTotal
) {}
