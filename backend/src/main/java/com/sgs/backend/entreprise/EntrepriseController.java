package com.sgs.backend.entreprise;

import com.sgs.backend.config.CurrentUserService;
import com.sgs.backend.config.SecurityRoles;
import com.sgs.backend.entreprise.dto.EntrepriseRegisterRequest;
import com.sgs.backend.entreprise.dto.EntrepriseRegisterResponse;
import com.sgs.backend.entreprise.dto.EntrepriseRequestDTO;
import com.sgs.backend.entreprise.dto.EntrepriseResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * EntrepriseController — gestion SELF-SERVICE du tenant.
 *
 * Modèle SaaS choisi : chaque entreprise se gouverne elle-même.
 *   - POST /register (PUBLIC)     : onboarding — crée l'entreprise ET son admin
 *                                   en une requête (cf. EntrepriseService.register).
 *   - GET  /me                    : mon entreprise (navbar, page réglages).
 *   - PUT  /me        (ADMIN)     : modifier SES informations.
 *   - DELETE /me      (ADMIN)     : supprimer SON entreprise (409 si données rattachées).
 *
 * Plus AUCUN endpoint d'énumération (ancien GET / listant toutes les
 * entreprises, ni GET/PUT/DELETE /{id} à id arbitraire) : même un ADMIN ne
 * peut pas découvrir ni manipuler le tenant d'un autre — le multi-tenant
 * s'applique aussi aux admins. L'id visé est TOUJOURS celui du token
 * (CurrentUserService), jamais un paramètre du client.
 */
@RestController
@RequestMapping("/api/entreprises")
@RequiredArgsConstructor
@Tag(name = "🏢 Entreprises", description = "Enregistrement public et gestion de son entreprise (multi-tenant)")
@SecurityRequirement(name = "bearerAuth")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;
    private final CurrentUserService currentUserService;

    /**
     * POST /api/entreprises/register — PUBLIC (cf. SecurityConfig).
     *
     * Onboarding : crée l'entreprise + son compte ADMIN en une transaction.
     * Le rôle ADMIN est forcé côté serveur (AdminRegisterDTO ne porte pas de
     * rôle) — jamais lu depuis le payload d'une requête anonyme.
     *
     * Réponse 201 : id/nom de l'entreprise + login de l'admin créé. Ni mot de
     * passe, ni token : l'admin se connecte ensuite sur l'écran de login
     * classique avec son email et son mot de passe.
     */
    @PostMapping("/register")
    // Volontairement SANS @PreAuthorize : endpoint public d'inscription.
    @Operation(
            summary = "🚀 Enregistrer une entreprise (public)",
            description = "Crée une entreprise ET son compte administrateur en une seule requête. "
                    + "L'admin se connecte ensuite avec son email et son mot de passe sur l'écran de login.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "✅ Entreprise et admin créés"),
                    @ApiResponse(responseCode = "409", description = "❌ Nom d'entreprise ou email admin déjà pris"),
                    @ApiResponse(responseCode = "400", description = "❌ Champs requis manquants ou invalides")
            }
    )
    public ResponseEntity<EntrepriseRegisterResponse> register(
            @Parameter(description = "Informations de l'entreprise + du futur admin", required = true)
            @Valid @RequestBody EntrepriseRegisterRequest request
    ) {
        EntrepriseRegisterResponse response = entrepriseService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/entreprises/me — l'entreprise de l'utilisateur connecté.
     *
     * Utilisé par le frontend (navbar, page réglages) : l'id vient du token,
     * il n'est jamais demandé au client.
     */
    @GetMapping("/me")
    @PreAuthorize(SecurityRoles.TOUS)
    @Operation(summary = "🔍 Mon entreprise (celle du token)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<EntrepriseResponseDTO> findMine() {
        return ResponseEntity.ok(entrepriseService.findById(currentUserService.getEntrepriseId()));
    }

    /**
     * PUT /api/entreprises/me — modifier SA propre entreprise (ADMIN).
     */
    @PutMapping("/me")
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "✏️ Modifier mon entreprise", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<EntrepriseResponseDTO> updateMine(
            @Parameter(description = "Nouvelles informations", required = true)
            @Valid @RequestBody EntrepriseRequestDTO dto
    ) {
        return ResponseEntity.ok(entrepriseService.update(currentUserService.getEntrepriseId(), dto));
    }

    /**
     * DELETE /api/entreprises/me — supprimer SA propre entreprise (ADMIN).
     *
     * 409 si des données sont encore rattachées (utilisateurs, articles...) :
     * la contrainte FK refuse, et le GlobalExceptionHandler renvoie un 409 JSON.
     */
    @DeleteMapping("/me")
    @PreAuthorize(SecurityRoles.ADMIN)
    @Operation(summary = "🗑️ Supprimer mon entreprise", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "204", description = "✅ Entreprise supprimée")
    public ResponseEntity<Void> deleteMine() {
        entrepriseService.delete(currentUserService.getEntrepriseId());
        return ResponseEntity.noContent().build();
    }
}
