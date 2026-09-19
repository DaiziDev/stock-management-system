package com.sgs.backend.config;

import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.config.dto.CurrentUserResponse;
import com.sgs.backend.config.dto.LoginRequest;
import com.sgs.backend.config.dto.LoginResponse;
import com.sgs.backend.config.dto.RefreshRequest;
import com.sgs.backend.config.dto.RegisterRequest;
import com.sgs.backend.entreprise.Entreprise;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController — endpoints d'authentification.
 *
 * Ce controller contient les SEULS endpoints publics (hors Swagger) :
 * /login et /refresh.
 * Tous les autres endpoints de l'API nécessitent un JWT valide.
 *
 * Flow de connexion :
 *   1. Frontend envoie POST /api/auth/login { login, motDePasse }
 *   2. Spring Security vérifie le mot de passe via AuthenticationManager
 *   3. Si OK → on génère un JWT (15 min) + un refresh token (7 j)
 *   4. On retourne les deux tokens + les infos utilisateur
 *   5. Frontend stocke les deux tokens dans localStorage
 *   6. À chaque requête future → header Authorization: Bearer <token>
 *   7. Access token expiré → POST /api/auth/refresh { refreshToken }
 *      → nouveau couple access/refresh, sans redonner ses identifiants
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
    private final CurrentUserService currentUserService;

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
    // Pas de @PreAuthorize : seul endpoint volontairement public (cf. SecurityConfig).
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

        // Construire la réponse (tokens + infos utilisateur) — helper partagé
        // avec /refresh pour garantir la même forme de réponse aux deux flows.
        return ResponseEntity.ok(buildAuthResponse(utilisateur));
    }

    /**
     * POST /api/auth/refresh
     *
     * Échange un refresh token valide contre un NOUVEAU couple
     * access/refresh (rotation) — sans redonner ses identifiants.
     *
     * Vérifications :
     * 1. Signature + expiration du refresh token (validateRefreshToken)
     * 2. Type "refresh" (isRefreshToken, redondant avec #1 mais explicite)
     * 3. L'utilisateur existe TOUJOURS et son compte est TOUJOURS actif :
     *    relu en base à chaque refresh — un compte désactivé ou supprimé
     *    ne peut plus se rafraîchir, même avec un refresh token encore
     *    valable (le token ne porte pas l'état du compte).
     *
     * Sécurité :
     * - Endpoint PUBLIC mais le rate limiting login ne s'y applique pas :
     *   un refresh token est une preuve d'authentification en soi (volé, il
     *   vaut un compte jusqu'à expiration — d'où la rotation à chaque usage
     *   et la relecture du compte en base).
     * - Rotation systématique : l'ancien refresh token reste signé/valable
     *   jusqu'à expiration (stateless, pas de révocation unitaire) ; la
     *   rotation limite la fenêtre d'abus en rendant chaque token à usage
     *   unique en pratique.
     */
    @PostMapping("/refresh")
    @Operation(
            summary = "🔄 Rafraîchir le token",
            description = "Échange un refresh token valide contre un nouveau couple " +
                    "access/refresh. Le compte est relu en base : un compte désactivé " +
                    "ne peut plus se rafraîchir.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "✅ Nouveau couple de tokens retourné"),
                    @ApiResponse(responseCode = "401", description = "❌ Refresh token invalide, expiré ou compte désactivé")
            }
    )
    public ResponseEntity<LoginResponse> refresh(
            @Parameter(description = "Refresh token reçu au login/refresh précédent", required = true)
            @Valid @RequestBody RefreshRequest request
    ) {
        String refreshToken = request.refreshToken();

        // 1-2. Signature, expiration et type du refresh token
        if (!jwtUtil.validateRefreshToken(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Refresh token invalide ou expiré");
        }

        // 3. Le compte doit toujours exister ET être actif (relu en base).
        //    Utilisateur supprimé depuis l'émission du refresh token → 401
        //    (pas un 404 : pour le client, c'est un problème d'authentification,
        //    la ressource n'est pas "ailleurs" — cohérent avec le contrat
        //    d'erreur du login).
        String login = jwtUtil.extractEmail(refreshToken);
        Utilisateur utilisateur;
        try {
            utilisateur = utilisateurService.findByLogin(login);
        } catch (ResourceNotFoundException e) {
            throw new BadCredentialsException("Refresh token invalide ou expiré");
        }
        if (!utilisateur.isActif()) {
            throw new DisabledException("Compte désactivé");
        }

        // Rotation : nouveau couple access + refresh (les deux changent).
        return ResponseEntity.ok(buildAuthResponse(utilisateur));
    }

    /**
     * Génère le couple access/refresh + les infos utilisateur — le même
     * payload pour login et refresh (rotation).
     */
    private LoginResponse buildAuthResponse(Utilisateur utilisateur) {
        String token = jwtUtil.generateToken(
                utilisateur.getLogin(),
                utilisateur.getRole().name(),
                utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getId() : null
        );
        String refreshToken = jwtUtil.generateRefreshToken(utilisateur.getLogin());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getLogin(),
                utilisateur.getRole(),
                utilisateur.getEntreprise() != null ? utilisateur.getEntreprise().getId() : null
        );

        return new LoginResponse(token, refreshToken, userInfo);
    }

    /**
     * POST /api/auth/register
     *
     * Crée un nouvel utilisateur. Réservé aux ADMIN uniquement.
     *
     * Le mot de passe est hashé côté backend (jamais en clair en base).
     */
    @PostMapping("/register")
    @PreAuthorize(SecurityRoles.ADMIN)
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
            @Valid @RequestBody RegisterRequest request
    ) {
        // Le contrôle du rôle ADMIN est porté par @PreAuthorize ci-dessus :
        // il s'exécute AVANT d'entrer dans la méthode, donc avant toute
        // lecture en base — un non-admin n'atteint jamais ce code.

        // ⚠️ Multi-tenant : le tenant n'est PAS lu dans la requête (le champ
        // entrepriseId a été retiré du RegisterRequest). Il est forcé à
        // l'entreprise de l'APPELANT — un admin ne crée des comptes que pour
        // SON entreprise. Sans cette garde, un admin pourrait inscrire qui il
        // veut dans un tenant voisin, brisant l'isolation multi-tenant.
        Entreprise entreprise = currentUserService.getEntrepriseCourante();
        if (entreprise == null) {
            throw new ResourceNotFoundException("Aucune entreprise rattachée au compte appelant");
        }

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

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildAuthResponse(saved));
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
    @PreAuthorize(SecurityRoles.TOUS)
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
