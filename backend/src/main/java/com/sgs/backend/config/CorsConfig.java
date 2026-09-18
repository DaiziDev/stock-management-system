package com.sgs.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration CORS — quels navigateurs (quelles origines) ont le droit
 * d'appeler cette API.
 *
 * La liste est lue depuis `app.cors.allowed-origins` (application.yaml) pour
 * qu'un déploiement puisse déclarer son domaine de production sans recompiler.
 *
 * Note sur allowCredentials : volontairement absent (donc `false`).
 * L'authentification de cette API passe par le header `Authorization: Bearer`,
 * jamais par un cookie de session — `allowCredentials(true)` n'apporte donc
 * rien, et autoriserait le navigateur à joindre les cookies du domaine aux
 * requêtes cross-origin. C'est exactement le vecteur CSRF que `csrf().disable()`
 * (justifié en stateless) suppose absent.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * Origines autorisées, séparées par des virgules.
     *
     * Défaut : les deux serveurs de développement front —
     *   - 4200 : `ng serve` (frontend Angular de ce dépôt)
     *   - 5173 : `vite dev` (frontend React en cours de mise en place)
     *
     * En production, surcharger via la variable d'environnement
     * CORS_ALLOWED_ORIGINS (ex. "https://sgs.example.com").
     */
    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
