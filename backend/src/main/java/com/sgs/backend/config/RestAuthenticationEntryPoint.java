package com.sgs.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sgs.backend.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * AuthenticationEntryPoint — ce que reçoit une requête ANONYME (ou à token
 * invalide/expiré) qui atteint une règle anyRequest().authenticated().
 *
 * Sans ce bean, Spring Security renvoie son 403 par défaut (page d'erreur
 * brute, sans corps JSON) : indistinguable du 403 « rôle insuffisant »,
 * le frontend ne peut pas décider s'il doit tenter un refresh ou renvoyer
 * vers l'écran de login.
 *
 * Distinction de contrat désormais explicite :
 *   - 401 ICI  → « tu n'es pas authentifié » : token absent, expiré,
 *                falsifié, compte désactivé ou supprimé. Le client doit
 *                tenter POST /api/auth/refresh puis se reconnecter.
 *   - 403 (GlobalExceptionHandler) → « tu es authentifié mais ton rôle ne
 *                suffit pas » : se reconnecter n'y changera rien.
 *
 * Le corps réutilise le format ApiError de toute l'API (sérialisé par le
 * même ObjectMapper que les @RestControllerAdvice) — un seul format à
 * parser côté frontend. Écrit manuellement dans la réponse : ce code court
 * AVANT les controllers, GlobalExceptionHandler n'est pas atteignable ici.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ApiError error = new ApiError(
                Instant.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "Authentification requise : fournissez un jeton valide dans le header Authorization.",
                request.getRequestURI()
        );
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
