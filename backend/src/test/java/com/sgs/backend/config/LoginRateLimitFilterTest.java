package com.sgs.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires de LoginRateLimitFilter — sans contexte Spring :
 * le filtre est instancié directement avec ses deux paramètres
 * (max-failures, window-seconds) et exécuté sur des requêtes/réponses
 * mockées. La chaîne aval est simulée par un FilterChain qui fixe le
 * statut de réponse voulu (200, 401...), comme le ferait AuthController.
 */
class LoginRateLimitFilterTest {

    private static final int MAX_FAILURES = 3;
    private static final long WINDOW_SECONDS = 300;

    private LoginRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new LoginRateLimitFilter(MAX_FAILURES, WINDOW_SECONDS);
    }

    // ───────────── Helpers ─────────────

    private MockHttpServletRequest loginRequest(String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setServletPath("/api/auth/login");
        request.setRequestURI("/api/auth/login");
        request.setRemoteAddr(ip);
        return request;
    }

    /** Chaîne aval qui simule la réponse du controller (statut fixé). */
    private FilterChain chainReturning(int status) {
        return (req, res) -> ((MockHttpServletResponse) res).setStatus(status);
    }

    /** Exécute une requête de login depuis l'IP donnée, avec la chaîne aval fournie. */
    private MockHttpServletResponse doLogin(String ip, FilterChain chain)
            throws ServletException, IOException {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(loginRequest(ip), response, chain);
        return response;
    }

    /** Accès direct au registre interne (package-private) pour simuler le temps. */
    private ConcurrentHashMap<String, LoginRateLimitFilter.Attempt> attemptsMap() {
        return filter.attempts;
    }

    // ───────────── Cas nominal ─────────────

    @Test
    @DisplayName("Sous le seuil d'échecs, la requête traverse le filtre")
    void letsRequestPassBelowThreshold() throws Exception {
        MockHttpServletResponse response = doLogin("1.1.1.1", chainReturning(401));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    // ───────────── Blocage 429 ─────────────

    @Test
    @DisplayName("Après max-failures échecs, la tentative suivante reçoit un 429 avec Retry-After")
    void blocksAfterMaxFailures() throws Exception {
        for (int i = 0; i < MAX_FAILURES; i++) {
            doLogin("1.1.1.1", chainReturning(401));
        }

        MockHttpServletResponse response = doLogin("1.1.1.1", chainReturning(401));

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("Retry-After")).isNotBlank();
        assertThat(response.getContentAsString()).contains("Too Many Requests");
        assertThat(response.getContentAsString()).contains("/api/auth/login");
    }

    @Test
    @DisplayName("Le 429 court-circuite la chaîne : le mot de passe n'est pas testé")
    void blockedRequestNeverReachesDownstream() throws Exception {
        for (int i = 0; i < MAX_FAILURES; i++) {
            doLogin("2.2.2.2", chainReturning(401));
        }

        // La chaîne aval échouerait bruyamment si elle était atteinte.
        MockHttpServletResponse response = doLogin("2.2.2.2", (req, res) -> {
            throw new AssertionError("La chaîne aval ne doit pas être atteinte quand l'IP est bloquée");
        });

        assertThat(response.getStatus()).isEqualTo(429);
    }

    // ───────────── Reset sur succès ─────────────

    @Test
    @DisplayName("Une connexion réussie remet le compteur d'échecs à zéro")
    void successResetsFailureCounter() throws Exception {
        // 2 échecs (sous le seuil de 3)...
        doLogin("3.3.3.3", chainReturning(401));
        doLogin("3.3.3.3", chainReturning(401));
        // ...puis un succès : le compteur de l'IP est supprimé.
        doLogin("3.3.3.3", chainReturning(200));

        assertThat(attemptsMap()).doesNotContainKey("3.3.3.3");

        // 2 nouveaux échecs : toujours sous le seuil, pas de blocage.
        MockHttpServletResponse r1 = doLogin("3.3.3.3", chainReturning(401));
        MockHttpServletResponse r2 = doLogin("3.3.3.3", chainReturning(401));
        assertThat(r1.getStatus()).isEqualTo(401);
        assertThat(r2.getStatus()).isEqualTo(401);
    }

    // ───────────── Statuts non comptés ─────────────

    @Test
    @DisplayName("Les statuts autres que 200/401/403 ne comptent pas comme échec")
    void otherStatusesAreNotCounted() throws Exception {
        // Des 500 répétés : aucun échec compté, jamais de blocage.
        for (int i = 0; i < MAX_FAILURES + 2; i++) {
            MockHttpServletResponse response = doLogin("4.4.4.4", chainReturning(500));
            assertThat(response.getStatus()).isEqualTo(500);
        }

        assertThat(attemptsMap().get("4.4.4.4").failures.get()).isZero();
    }

    // ───────────── Expiration de fenêtre ─────────────

    @Test
    @DisplayName("Après expiration de la fenêtre, le compteur repart de zéro")
    void windowExpiryResetsCounter() throws Exception {
        for (int i = 0; i < MAX_FAILURES; i++) {
            doLogin("5.5.5.5", chainReturning(401));
        }
        // L'IP est bloquée :
        assertThat(doLogin("5.5.5.5", chainReturning(401)).getStatus()).isEqualTo(429);

        // Simulation : la fenêtre a démarré bien avant window-seconds.
        LoginRateLimitFilter.Attempt attempt = attemptsMap().get("5.5.5.5");
        attempt.windowStart.set(
                System.currentTimeMillis() - (WINDOW_SECONDS * 1000L) - 1000L);

        // La tentative repasse : fenêtre réinitialisée, requête traitée (401),
        // et le compteur redémarre à 1.
        MockHttpServletResponse response = doLogin("5.5.5.5", chainReturning(401));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(attempt.failures.get()).isEqualTo(1);
    }

    // ───────────── Isolation par IP ─────────────

    @Test
    @DisplayName("Les compteurs sont indépendants entre les IP")
    void countersAreIndependentPerIp() throws Exception {
        for (int i = 0; i < MAX_FAILURES; i++) {
            doLogin("6.6.6.6", chainReturning(401));
        }
        // 6.6.6.6 est bloquée...
        assertThat(doLogin("6.6.6.6", chainReturning(401)).getStatus()).isEqualTo(429);
        // ...mais pas 7.7.7.7.
        assertThat(doLogin("7.7.7.7", chainReturning(401)).getStatus()).isEqualTo(401);
    }

    // ───────────── X-Forwarded-For ─────────────

    @Test
    @DisplayName("X-Forwarded-For (première IP) prime sur remoteAddr")
    void usesFirstIpFromXForwardedFor() throws Exception {
        for (int i = 0; i < MAX_FAILURES; i++) {
            MockHttpServletRequest request = loginRequest("10.0.0.1");
            request.addHeader("X-Forwarded-For", "8.8.8.8, 10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chainReturning(401));
        }

        // L'IP forwardée est bloquée...
        assertThat(doLogin("ignored", chainReturning(401)).getStatus()).isEqualTo(401);
        MockHttpServletRequest viaProxy = loginRequest("10.0.0.1");
        viaProxy.addHeader("X-Forwarded-For", "8.8.8.8, 10.0.0.1");
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(viaProxy, blocked, chainReturning(401));
        assertThat(blocked.getStatus()).isEqualTo(429);

        // ...tandis que remoteAddr seul n'a aucun compteur.
        assertThat(attemptsMap()).doesNotContainKey("10.0.0.1");
    }

    // ───────────── Périmètre du filtre ─────────────

    @Test
    @DisplayName("Le filtre ne s'applique qu'à /api/auth/login")
    void onlyAppliesToLoginPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        request.setServletPath("/api/articles");
        request.setRemoteAddr("9.9.9.9");

        // shouldNotFilter → la chaîne aval est appelée telle quelle.
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chainReturning(401));

        assertThat(attemptsMap()).doesNotContainKey("9.9.9.9");
    }
}
