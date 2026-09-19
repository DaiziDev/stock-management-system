package com.sgs.backend.entreprise.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requête de l'enregistrement PUBLIC d'une entreprise
 * (POST /api/entreprises/register — le seul endpoint public de création).
 *
 * Une seule requête crée l'entreprise ET son compte administrateur :
 * l'admin naît avec l'entreprise (transaction unique, cf. EntrepriseService.register).
 *
 * Champs requis (décision produit) : nom, email et téléphone de l'entreprise,
 * + le bloc admin complet. L'adresse est optionnelle.
 */
public record EntrepriseRegisterRequest(

        @NotBlank(message = "Le nom de l'entreprise est obligatoire")
        String nom,

        @NotBlank(message = "L'email de l'entreprise est obligatoire")
        @Email(message = "L'email de l'entreprise doit être valide")
        String mail,

        @NotBlank(message = "Le téléphone de l'entreprise est obligatoire")
        String numTel,

        String adresse1,
        String adresse2,
        String ville,
        String codePostal,
        String pays,

        @Valid
        @NotNull(message = "Les informations de l'administrateur sont obligatoires")
        AdminRegisterDTO admin
) {}
