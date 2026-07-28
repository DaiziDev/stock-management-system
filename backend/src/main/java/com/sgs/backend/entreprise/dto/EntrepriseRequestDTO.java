package com.sgs.backend.entreprise.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// On met les champs d'adresse à plat dans le DTO plutôt que d'imbriquer un
// objet AdresseDTO -- pour un formulaire simple côté frontend (un seul
// niveau de champs), c'est plus direct à mapper qu'un sous-objet.
// C'est le Service qui reconstruit l'objet Adresse (@Embeddable) à partir
// de ces champs plats avant de le mettre dans l'entité.
public record EntrepriseRequestDTO(
        @NotBlank(message = "le nom de l'entreprise est obligatoire")
        String nom,

        String adresse1,
        String adresse2,
        String ville,
        String codePostal,
        String pays,

        @Email(message = "l'adresse mail doit être valide")
        String mail,

        String numTel
) {}
