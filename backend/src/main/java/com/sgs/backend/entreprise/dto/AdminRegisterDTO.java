package com.sgs.backend.entreprise.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Bloc administrateur embarqué dans la requête d'enregistrement public
 * d'une entreprise (POST /api/entreprises/register).
 *
 * Le rôle n'existe PAS ici : il est forcé à ADMIN côté service, jamais lu
 * depuis le client — sinon n'importe qui pourrait créer un compte du rôle
 * de son choix sur un endpoint public.
 *
 * L'email sert de login (décision produit) : un seul champ pour les deux.
 */
public record AdminRegisterDTO(

        @NotBlank(message = "Le nom de l'administrateur est obligatoire")
        String nom,

        @NotBlank(message = "Le prénom de l'administrateur est obligatoire")
        String prenom,

        @NotBlank(message = "L'email de connexion est obligatoire")
        @Email(message = "L'email de connexion doit être valide")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String motDePasse
) {}
