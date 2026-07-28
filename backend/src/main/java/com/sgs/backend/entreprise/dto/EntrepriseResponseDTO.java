package com.sgs.backend.entreprise.dto;

public record EntrepriseResponseDTO(
        Long id,
        String nom,
        String adresse1,
        String adresse2,
        String ville,
        String codePostal,
        String pays,
        String mail,
        String numTel
) {}
