package com.sgs.backend.utilisateur;

import com.sgs.backend.config.SecurityRoles;
import com.sgs.backend.utilisateur.dto.UtilisateurResponseDTO;
import com.sgs.backend.utilisateur.dto.UtilisateurUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Réservé aux ADMIN -- la création reste sur POST /api/auth/register
 * (qui applique déjà cette même vérification), ce controller ne couvre que
 * lister/consulter/modifier/supprimer un compte de la même entreprise.
 *
 * Le @PreAuthorize est posé au niveau de la CLASSE : toutes les méthodes sont
 * réservées au même rôle, et une méthode ajoutée plus tard hérite de la règle
 * au lieu de partir sans protection.
 *
 * L'appartenance à l'entreprise reste vérifiée méthode par méthode (via
 * l'entrepriseId de l'utilisateur courant) : @PreAuthorize répond à "quel
 * rôle ?", pas à "quel tenant ?" — un ADMIN d'une autre entreprise passe le
 * contrôle de rôle et doit être arrêté par le filtrage multi-tenant.
 */
@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@Tag(name = "👥 Utilisateurs", description = "Gestion des comptes (ADMIN uniquement)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize(SecurityRoles.ADMIN)
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    @Operation(summary = "📋 Lister les utilisateurs de mon entreprise")
    public ResponseEntity<List<UtilisateurResponseDTO>> findAll(@AuthenticationPrincipal UserDetails currentUser) {
        Utilisateur moi = utilisateurService.findByLogin(currentUser.getUsername());
        List<UtilisateurResponseDTO> utilisateurs = utilisateurService.findAllByEntreprise(moi.getEntreprise().getId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
        return ResponseEntity.ok(utilisateurs);
    }

    @GetMapping("/{id}")
    @Operation(summary = "🔍 Détail d'un utilisateur")
    public ResponseEntity<UtilisateurResponseDTO> findById(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        Utilisateur moi = utilisateurService.findByLogin(currentUser.getUsername());
        return ResponseEntity.ok(toResponseDTO(utilisateurService.findByIdAndEntreprise(id, moi.getEntreprise().getId())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "✏️ Modifier un utilisateur", description = "Nom, prénom, contact et rôle. Ni login ni mot de passe (endpoints séparés).")
    public ResponseEntity<UtilisateurResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UtilisateurUpdateDTO dto,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        Utilisateur moi = utilisateurService.findByLogin(currentUser.getUsername());
        Utilisateur updated = utilisateurService.update(id, moi.getEntreprise().getId(), dto, moi);
        return ResponseEntity.ok(toResponseDTO(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "🗑️ Supprimer un utilisateur")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUser) {
        Utilisateur moi = utilisateurService.findByLogin(currentUser.getUsername());
        utilisateurService.delete(id, moi.getEntreprise().getId());
        return ResponseEntity.noContent().build();
    }

    private UtilisateurResponseDTO toResponseDTO(Utilisateur utilisateur) {
        return new UtilisateurResponseDTO(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getLogin(),
                utilisateur.getMail(),
                utilisateur.getNumTel(),
                utilisateur.getRole(),
                utilisateur.isActif()
        );
    }
}
