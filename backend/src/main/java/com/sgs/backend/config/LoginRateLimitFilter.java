package com.sgs.backend.config;

import com.sgs.backend.common.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiting sur POST /api/auth/login — le seul endpoint public de l'API,
 * donc la seule porte d'attaque « offline » d'un brute force de mot de passe.
 *
 * Algorithme : fenêtre glissante par IP.
 *   - Chaque IP a un compteur d'échecs + la date du début de sa fenêtre.
 *   - Tant que la fenêtre n'est pas expirée, chaque échec incrémente le
 *     compteur ; au-delà de {@code max-failures}, toute nouvelle tentative
 *     reçoit un 429 AVANT d'atteindre AuthenticationManager (le mot de passe
 *     n'est même pas testé — c'est ce qui rend le brute force impraticable).
 *   - Une connexion RÉUSSIE remet le compteur à zéro : un utilisateur légitime
 *     qui se trompe deux fois puis réussit n'hérite d'aucune pénalité.
 *   - Après expiration de la fenêtre ({@code window-seconds}), le compteur
 *     repart de zéro : le blocage est temporaire, pas définitif.
 *
 * Compter les ÉCHECS (et non les requêtes) plutôt que limiter toutes les
 * tentatives évite de verrouiller un open space entier derrière une IP NAT :
 * 40 connexions réussies en 15 minutes y sont un usage parfaitement normal.
 *
 * Stockage en mémoire ({@code ConcurrentHashMap}) : suffisant pour une
 * instance mono-nœud. Si l'application passe en multi-instances derrière un
 * load balancer, ce compteur devra être déplacé vers un store partagé
 * (Redis, bucket4j + cache…) — le filtre est construit pour être remplacé
 * sans changer le contrat (429 + Retry-After).
 *
 * Enregistrement : le filtre vit UNIQUEMENT dans la chaîne Spring Security
 * (posé par SecurityConfig avant JwtAuthFilter). Le @Component rend le bean
 * injectable, mais l'enregistrement automatique de Spring Boot dans la chaîne
 * de servlets est désactivé par un FilterRegistrationBean(enabled=false) dans
 * SecurityConfig — sinon le filtre s'exécuterait DEUX fois par requête
 * (chaîne de servlets PUIS FilterChainProxy) et chaque échec serait compté
 * double, divisant d'autant le nombre de tentatives avant blocage.
 */
@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    /** Chemin surveillé — seul endpoint public de l'API (cf. SecurityConfig). */
    private static final String LOGIN_PATH = "/api/auth/login";

    private final int maxFailures;
    private final long windowSeconds;

    /**
     * État par IP : compteur d'échecs + début de la fenêtre courante (epoch ms).
     * ConcurrentHashMap suffit : une seule écriture atomique par clé, pas de
     * structure imbriquée à verrouiller.
     *
     * Visibilité package-private : la classe est un détail d'implémentation,
     * mais les tests unitaires (même package) simulent l'écoulement du temps
     * en réécrivant windowStart — un mock d'horloge serait plus lourd que ça
     * n'en vaut la peine ici.
     */
    final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public LoginRateLimitFilter(
            @Value("${app.rate-limit.login.max-failures:5}") int maxFailures,
            @Value("${app.rate-limit.login.window-seconds:300}") long windowSeconds
    ) {
        this.maxFailures = maxFailures;
        this.windowSeconds = windowSeconds;
    }

    /** État d'une IP : échecs dans la fenêtre courante + début de fenêtre. */
    static final class Attempt {
        final AtomicInteger failures = new AtomicInteger();
        final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Ne s'exécute QUE sur le login — aucune latence ajoutée ailleurs.
        return !LOGIN_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String ip = clientIp(request);
        long now = System.currentTimeMillis();
        cleanupExpired(now);

        Attempt attempt = attempts.computeIfAbsent(ip, k -> new Attempt());

        // Fenêtre expirée → nouvelle fenêtre, compteur reparti de zéro.
        // compareAndSet/Set atomiques : deux requêtes simultanées ne peuvent
        // pas réinitialiser le compteur l'une de l'autre.
        long windowStart = attempt.windowStart.get();
        if (now - windowStart > windowSeconds * 1000L) {
            if (attempt.windowStart.compareAndSet(windowStart, now)) {
                attempt.failures.set(0);
            }
        }

        // Blocage : la fenêtre est encore valide et le plafond d'échecs est atteint.
        if (attempt.failures.get() >= maxFailures) {
            long retryAfter = Math.max(1,
                    (attempt.windowStart.get() + windowSeconds * 1000L - now + 999) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiError error = new ApiError(
                    Instant.now(),
                    429,
                    "Too Many Requests",
                    "Trop de tentatives de connexion échouées. Réessayez dans "
                            + retryAfter + " seconde(s).",
                    request.getRequestURI()
            );
            // Écrit manuellement (pas de controller derrière ce refus — le filtre
            // court-circuite la chaîne, GlobalExceptionHandler n'est pas atteint).
            response.getWriter().write(
                    "{\"timestamp\":\"" + error.timestamp() + "\",\"status\":429,"
                            + "\"error\":\"Too Many Requests\",\"message\":\""
                            + error.message() + "\",\"path\":\"" + error.path() + "\"}");
            return; // Requête stoppée ici : n'atteint jamais AuthenticationManager.
        }

        filterChain.doFilter(request, response);

        // Post-traitement : la réponse est disponible après la chaîne.
        int status = response.getStatus();
        if (status == 200) {
            // Succès → l'IP repart de zéro (suppression : l'entrée sera
            // recréée à la prochaine tentative, pas de compteur fantôme).
            attempts.remove(ip);
        } else if (status == 401 || status == 403) {
            // Échec d'authentification (401 = mauvais identifiants, 403 = compte
            // désactivé...) → compteur d'échecs +1.
            attempt.failures.incrementAndGet();
        }
        // Les autres statuts (500, 400 de validation...) ne comptent pas :
        // ils ne signalent pas une tentative de devine mot de passe.
    }

    /**
     * IP du client. Derrière un reverse proxy, remoteAddr est celle du proxy :
     * X-Forwarded-For (format "client, proxy1, proxy2") porte la vraie IP en
     * premier. ⚠️ Ce header est falsifiable si la requête n'est PAS passée par
     * un proxy de confiance — dans ce cas, chaque fausse IP crée un compteur
     * vierge et contourne la limite. Dès qu'un reverse proxy sera mis en place
     * devant l'API, il devra écraser (et non concaténer) ce header.
     */
    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Purge opportuniste des entrées dont la fenêtre est expirée. Sans elle,
     * la map grossirait avec chaque IP distincte (fuite mémoire lente sous
     * scan). Exécutée sur le thread appelant, en dehors des cas courants :
     * amortie, pas de thread dédié pour un besoin aussi modeste.
     */
    private void cleanupExpired(long now) {
        if (attempts.size() < 1024) {
            return;
        }
        long expiry = windowSeconds * 1000L;
        Iterator<Map.Entry<String, Attempt>> it = attempts.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue().windowStart.get() > expiry) {
                it.remove();
            }
        }
    }
}
