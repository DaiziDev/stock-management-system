package com.sgs.backend.common;

import com.sgs.backend.mvtStk.StockInsuffisantException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DisabledException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

// @RestControllerAdvice intercepte les exceptions levées PAR N'IMPORTE QUEL
// controller de l'application. Sans ça, une exception non gérée renvoie
// une page d'erreur Spring par défaut (ou une stacktrace) au lieu d'un JSON propre.
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Ressource introuvable (ex: GET /api/categories/999 qui n'existe pas) -> 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Identifiants invalides sur POST /api/auth/login (authenticationManager.authenticate()
    // lève BadCredentialsException, une sous-classe d'AuthenticationException) -> 401.
    // Sans ce handler, Spring MVC ne sait pas convertir cette exception en réponse
    // HTTP propre et renvoie un 500 générique -- trompeur pour un simple mauvais mot de passe.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Identifiant ou mot de passe incorrect",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    // Rôle insuffisant pour l'action demandée : @PreAuthorize a refusé l'appel
    // (ex. un VENDEUR qui tente POST /api/articles) -> 403.
    // Sans ce handler la requête ressort jusqu'à ExceptionTranslationFilter, qui
    // renvoie bien un 403 mais au format d'erreur par défaut de Spring — le
    // frontend recevrait une forme de corps différente de tous les autres refus.
    //
    // 403 et non 401 : l'utilisateur EST authentifié (les requêtes anonymes sont
    // déjà arrêtées en amont par `anyRequest().authenticated()`), c'est son rôle
    // qui ne suffit pas. Se reconnecter n'y changerait rien.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "Votre rôle ne vous autorise pas cette action",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // Compte désactivé (isEnabled() == false) au LOGIN : DaoAuthenticationProvider
    // lève DisabledException une fois le mot de passe vérifié. 403 et non 401 :
    // les identifiants sont bons — c'est l'état du compte qui interdit l'accès.
    // (Sur les requêtes avec token, JwtAuthFilter n'authentifie pas les comptes
    // désactivés ; la requête ressort en 401 "non authentifié" avant tout handler.)
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex, HttpServletRequest req) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "Compte désactivé. Contactez un administrateur.",
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // Refus métier attendu : code déjà utilisé, statut de commande incompatible
    // avec l'action demandée ("déjà validée", "déjà reçue"...) -> 400.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }

    // Stock insuffisant pour une SORTIE (vente, validation de commande client) -> 409.
    // 409 (Conflict) plutôt que 400 : la requête est valide, c'est l'état actuel
    // du stock qui empêche de l'honorer -- RG-05 du cahier des charges.
    @ExceptionHandler(StockInsuffisantException.class)
    public ResponseEntity<ApiError> handleStockInsuffisant(StockInsuffisantException ex, HttpServletRequest req) {
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                req.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // Erreurs de validation (@NotNull, @NotBlank... sur un DTO annoté @Valid) -> 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        ApiError error = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                details,
                req.getRequestURI()
        );
        return ResponseEntity.badRequest().body(error);
    }
}
