package com.sgs.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration Spring Security.
 *
 * Principe clé : STATELESS (pas de session côté serveur).
 * Chaque requête doit contenir le JWT dans le header Authorization.
 * Le serveur ne garde AUCUNE trace de connexion entre les requêtes.
 *
 * Le PasswordEncoder est défini dans PasswordConfig pour éviter
 * la dépendance circulaire SecurityConfig → JwtAuthFilter → UtilisateurService
 * → PasswordEncoder → SecurityConfig.
 *
 * L'autorisation se fait sur DEUX niveaux complémentaires :
 *   1. Ici (niveau URL) — "es-tu authentifié ?". Grossier, mais c'est le
 *      filet de sécurité : un endpoint oublié reste fermé aux anonymes.
 *   2. @PreAuthorize sur chaque méthode de controller (@EnableMethodSecurity
 *      ci-dessous) — "ton rôle t'autorise-t-il CETTE action ?". Les
 *      expressions sont centralisées dans SecurityRoles.
 *
 * Le niveau 2 ne remplace pas le niveau 1 : sans `anyRequest().authenticated()`,
 * un controller ajouté sans @PreAuthorize serait public.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final LoginRateLimitFilter loginRateLimitFilter;

    /**
     * AuthenticationManager est requis par Spring Security pour
     * l'authentification classique (username/password).
     * On l'expose ici pour pouvoir l'utiliser dans AuthController.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Désactive l'enregistrement automatique du LoginRateLimitFilter par le
     * conteneur de servlets : le filtre est déjà posé dans la chaîne Spring
     * Security (addFilterBefore ci-dessous). Sans ce bean, il s'exécuterait
     * deux fois par requête (chaîne de servlets PUIS FilterChainProxy) et
     * chaque échec de login serait compté double.
     */
    @Bean
    public FilterRegistrationBean<LoginRateLimitFilter> loginRateLimitFilterRegistration(
            LoginRateLimitFilter loginRateLimitFilter
    ) {
        FilterRegistrationBean<LoginRateLimitFilter> registration =
                new FilterRegistrationBean<>(loginRateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * Chaîne de filtres de sécurité — le cœur de la config.
     *
     * L'ordre des filtres compte :
     * 1. CorsConfig (déjà défini séparément)
     * 2. Désactivation du CSRF (inutile en API stateless)
     * 3. Pas de session (STATELESS)
     * 4. Règles d'autorisation sur les endpoints (grossières — le détail
     *    par rôle est porté par @PreAuthorize sur les controllers)
     * 5. LoginRateLimitFilter AVANT JwtAuthFilter (anti brute force sur /login)
     * 6. NotreJwtFilter AVANT UsernamePasswordAuthenticationFilter
 */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Désactiver CSRF — inutile pour une API REST stateless
            // Le CSRF protège contre les formulaires HTML volant des tokens de session
            // Comme on utilise JWT (pas de cookie de session), pas besoin
            .csrf(AbstractHttpConfigurer::disable)

            // Configurer les règles d'accès aux endpoints
            .authorizeHttpRequests(auth -> auth
                // ── Endpoints PUBLICS (pas besoin d'être connecté) ──
                // Seul /login est public : /register et /me lisent
                // @AuthenticationPrincipal et plantent (NPE -> 500) si appelés
                // sans JWT -- ils doivent passer par la règle "authenticated"
                // ci-dessous pour recevoir un 401/403 propre à la place.
                .requestMatchers(
                        "/api/auth/login",
                        "/swagger-ui.html",       // Swagger UI
                        "/swagger-ui/**",         // Swagger UI ressources
                        "/api-docs/**",           // OpenAPI docs
                        "/v3/api-docs/**"         // OpenAPI docs v3
                ).permitAll()

                // ── Tout le reste nécessite une authentification ──
                .anyRequest().authenticated()
            )

            // Pas de session côté serveur — chaque requête est indépendante
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Ajouter notre filtre JWT AVANT le filtre standard Spring Security
            // Cela permet d'authentifier les requêtes avec notre token
            // NB : enregistré EN PREMIER — addFilterBefore(filtre, X.class) exige
            // que la classe X soit déjà connue de la chaîne, on ne peut donc pas
            // référencer JwtAuthFilter avant de l'avoir posé ci-dessous.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // Rate limiting du login AVANT le filtre JWT : une IP bloquée reçoit
            // son 429 sans déclencher la validation du token ni l'authentification.
            .addFilterBefore(loginRateLimitFilter, JwtAuthFilter.class);

        return http.build();
    }
}
