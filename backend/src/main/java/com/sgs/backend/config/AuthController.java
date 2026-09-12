package com.sgs.backend.config;

import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.config.dto.CurrentUserResponse;
import com.sgs.backend.config.dto.LoginRequest;
import com.sgs.backend.config.dto.LoginResponse;
import com.sgs.backend.config.dto.RegisterRequest;
import com.sgs.backend.entreprise.Entreprise;
import com.sgs.backend.entreprise.EntrepriseRepository;
import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.Utilisateur;
import com.sgs.backend.utilisateur.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController — endpoints d'authentification.
 *
 * Ce controller est le SEUL endpoint public (hors Swagger).
 * Tous les autres endpoints de l'API nécessitent un JWT valide.
 *
 * Flow de connexion :
 *   1. Frontend envoie POST /api/auth/login { login, motDePasse }
 *   2. Spring Security vérifie le mot de passe via AuthenticationManager
 *   3. Si OK → on génère un JWT avec email, rôle, entrepriseId
 *   4. On retourne le token + les infos utilisateur
 *   5. Frontend stocke le token dans localStorage
 *   6. À chaque requête future → header Authorization: Bearer <token>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "🔐 Authentification", description = "Connexion, inscription et profil utilisateur")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UtilisateurService utilisateurService;
    private final UserDetailsService userDetailsService;
    private final EntrepriseRepository entrepriseRepository;

    /**
     * POST /api/auth/login
     *
     * Authentifie un utilisateur et retourne un JWT.
     *
     * Le flow interne :
     * 1. AuthenticationManager vérifie login + mot de passe via UserDetailsService
     * 2. Si les identifiants sont corrects → on obtient un objet Authentication
     * 3. On génère un JWT contenant l'email, le rôle et l'entrepriseId
     * 4. On retourne le token + les infos utilisateur (sans le mot de passe)
     */
    @PostMapping("/login")
    @Operation(
            summary = "🔑 Connexion",
            description = "Authentifie un utilisateur avec son login et mot de passe. " +
                    "Retourne un token JWT à utiliser dans le header Authorization pour les requêtes suivantes.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "✅ Connexion réussie — token JWT retourné"),
                    @ApiResponse(responseCode = "403", description = "❌ Identifiants incorrects")
            }
    )
    public ResponseEntity<LoginResponse> login(
            @Parameter(description = "Identifiants de connexion", required = true)
            @Valid @RequestBody LoginRequest request
    ) {
        // Spring Security vérifie le login + mot de passe
        // Si échoue → lance BadCredentialsException (403 automatique)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.login(),
                        request.motDePasse()
                )
        );

        // Charger les détails complets de l'utilisateur
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Utilisateur utilisateur = utilisateurService.findByLogin(userDetails.getUsername());

        // Générer le JWT avec les infos nécessaires
        String token = jwtUtil.generateToken(
                utilisateur.getLogin(),
                utilisateur.getRole().name(),
                utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getId() : null
        );

        // Construire la réponse (token + infos utilisateur)
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getLogin(),
                utilisateur.getRole(),
                utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getId() : null
        );

        return ResponseEntity.ok(new LoginResponse(token, userInfo));
    }

    /**
     * POST /api/auth/register
     *
     * Crée un nouvel utilisateur. Réservé aux ADMIN uniquement.
     *
     * Le mot de passe est hashé côté backend (jamais en clair en base).
     */
    @PostMapping("/register")
    @Operation(
            summary = "👤 Inscription (Admin uniquement)",
            description = "Crée un nouvel utilisateur. Réservé aux administrateurs.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "201", description = "✅ Utilisateur créé"),
                    @ApiResponse(responseCode = "403", description = "❌ Accès refusé (non admin)")
            }
    )
    public ResponseEntity<LoginResponse> register(
            @Parameter(description = "Données du nouvel utilisateur", required = true)
            @Valid @RequestBody RegisterRequest request,
            @AuthenticationPrincipal UserDetails currentUser
    ) {
        // Vérifier que l'utilisateur connecté est bien un ADMIN
        Utilisateur admin = utilisateurService.findByLogin(currentUser.getUsername());
        if (admin.getRole() != UserRole.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Vérifier que l'entreprise existe
        Entreprise entreprise = entrepriseRepository.findById(request.entrepriseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Entreprise introuvable avec id=" + request.entrepriseId()
                ));

        // Créer l'utilisateur (le mot de passe sera hashé dans UtilisateurService.create)
        Utilisateur newUser = new Utilisateur();
        newUser.setNom(request.nom());
        newUser.setPrenom(request.prenom());
        newUser.setLogin(request.login());
        newUser.setMotDePasse(request.motDePasse());
        newUser.setMail(request.mail());
        newUser.setNumTel(request.numTel());
        newUser.setRole(request.role());
        newUser.setEntreprise(entreprise);

        Utilisateur saved = utilisateurService.create(newUser);

        // Générer un token pour le nouvel utilisateur
        String token = jwtUtil.generateToken(
                saved.getLogin(),
                saved.getRole().name(),
                saved.getEntreprise() != null ? saved.getEntreprise().getId() : null
        );

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                saved.getId(),
                saved.getNom(),
                saved.getPrenom(),
                saved.getLogin(),
                saved.getRole(),
                saved.getEntreprise() != null ? saved.getEntreprise().getId() : null
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new LoginResponse(token, userInfo));
    }

    /**
     * GET /api/auth/me
     *
     * Retourne les informations de l'utilisateur connecté.
     * Le token JWT est automatiquement vérifié par JwtAuthFilter.
     *
     * Utilisé par le frontend lors du refresh de la page
     * pour recharger le profil sans re-demander les identifiants.
     */
    @GetMapping("/me")
    @Operation(
            summary = "👤 Mon profil",
            description = "Retourne les informations de l'utilisateur connecté (déduit du token JWT).",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "✅ Profil retourné"),
                    @ApiResponse(responseCode = "401", description = "❌ Non authentifié")
            }
    )
    public ResponseEntity<CurrentUserResponse> me(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Utilisateur utilisateur = utilisateurService.findByLogin(userDetails.getUsername());

        return ResponseEntity.ok(new CurrentUserResponse(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getLogin(),
                utilisateur.getMail(),
                utilisateur.getNumTel(),
                utilisateur.getRole(),
                utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getId() : null
        ));
    }
}
