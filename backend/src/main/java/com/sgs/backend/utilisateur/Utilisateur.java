package com.sgs.backend.utilisateur;

import com.sgs.backend.common.AbstractEntity;
import com.sgs.backend.entreprise.Entreprise;
import com.sgs.backend.roles.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "utilisateur")
public class Utilisateur extends AbstractEntity {

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "prenom", nullable = false)
    private String prenom;

    @Column(name = "login", nullable = false, unique = true)
    private String login;

    @Column(name = "motdepasse", nullable = false)
    private String motDePasse;

    @Column(name = "mail")
    private String mail;

    @Column(name = "numtel")
    private String numTel;

    @Column(name = "photo")
    private String photo;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role;

    /**
     * Compte activé ou désactivé. false = le compte ne peut plus ni se
     * connecter ni utiliser un token encore valide (contrôlé à chaque
     * requête par JwtAuthFilter via UserDetails.isEnabled()).
     *
     * Le DEFAULT true en base est nécessaire : quand ddl-auto=update ajoute
     * la colonne sur une table qui contient déjà des lignes, PostgreSQL
     * exige une valeur par défaut, sinon l'ALTER échoue (valeurs NULL).
     * Un ADMIN qui se voit désactiver son propre compte par défaut est
     * arrêté par la garde du service, pas par ce défaut.
     */
    @Column(name = "actif", nullable = false, columnDefinition = "boolean not null default true")
    private boolean actif = true;

    @ManyToOne
    @JoinColumn(name = "identreprise")
    private Entreprise entreprise;
}
