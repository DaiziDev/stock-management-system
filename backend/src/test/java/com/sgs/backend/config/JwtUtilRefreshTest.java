package com.sgs.backend.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitaires de JwtUtil pour la partie refresh token — sans contexte
 * Spring : l'instance est créée directement, les champs @Value injectés par
 * réflexion (même pattern que LoginRateLimitFilterTest).
 *
 * Points couverts :
 * - Un refresh token est typ=refresh et porte le seul le login
 * - Un access token n'a PAS de claim typ (l'absence l'identifie)
 * - validateRefreshToken refuse access token, token expiré, signature fausse
 * - Un refresh token refusé comme access token (via isRefreshToken)
 */
class JwtUtilRefreshTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "test-secret-jwt-refresh-0123456789abcdef-0123456789abcdef".getBytes());
    private static final long ACCESS_MS = 15 * 60 * 1000L;   // 15 min
    private static final long REFRESH_MS = 7 * 24 * 3600 * 1000L; // 7 jours

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil();
        var secretField = JwtUtil.class.getDeclaredField("secretKey");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, SECRET);
        var expField = JwtUtil.class.getDeclaredField("jwtExpiration");
        expField.setAccessible(true);
        expField.set(jwtUtil, ACCESS_MS);
        var refreshField = JwtUtil.class.getDeclaredField("refreshExpiration");
        refreshField.setAccessible(true);
        refreshField.set(jwtUtil, REFRESH_MS);
        // @PostConstruct n'est pas appelé hors Spring — la validation du
        // secret est couverte par le démarrage réel de l'application.
    }

    // ───────────── Génération ─────────────

    @Test
    @DisplayName("Le refresh token porte typ=refresh et le login en subject")
    void refreshTokenCarriesTypeAndSubject() {
        String refresh = jwtUtil.generateRefreshToken("user@sgs.local");

        assertThat(jwtUtil.isRefreshToken(refresh)).isTrue();
        assertThat(jwtUtil.extractEmail(refresh)).isEqualTo("user@sgs.local");
    }

    @Test
    @DisplayName("Le refresh token ne porte NI rôle NI entrepriseId")
    void refreshTokenHasNoRoleNoEntreprise() {
        String refresh = jwtUtil.generateRefreshToken("user@sgs.local");

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET)))
                .build()
                .parseSignedClaims(refresh)
                .getPayload();

        assertThat(claims.get("role")).isNull();
        assertThat(claims.get("entrepriseId")).isNull();
        assertThat(claims.get("typ")).isEqualTo("refresh");
    }

    @Test
    @DisplayName("L'access token n'a pas de claim typ (n'est pas un refresh)")
    void accessTokenIsNotRefreshToken() {
        String access = jwtUtil.generateToken("user@sgs.local", "ADMIN", 42L);

        assertThat(jwtUtil.isRefreshToken(access)).isFalse();
    }

    // ───────────── Validation ─────────────

    @Test
    @DisplayName("validateRefreshToken accepte un refresh token frais")
    void validatesFreshRefreshToken() {
        String refresh = jwtUtil.generateRefreshToken("user@sgs.local");

        assertThat(jwtUtil.validateRefreshToken(refresh)).isTrue();
    }

    @Test
    @DisplayName("validateRefreshToken refuse un access token")
    void rejectsAccessTokenAsRefresh() {
        String access = jwtUtil.generateToken("user@sgs.local", "ADMIN", 42L);

        assertThat(jwtUtil.validateRefreshToken(access)).isFalse();
    }

    @Test
    @DisplayName("validateRefreshToken refuse un token expiré")
    void rejectsExpiredRefreshToken() throws Exception {
        // Refresh token émis "il y a 8 jours" (durée 7 j) → expiré.
        var refreshField = JwtUtil.class.getDeclaredField("refreshExpiration");
        refreshField.setAccessible(true);
        refreshField.set(jwtUtil, -8 * 24 * 3600 * 1000L);

        String expired = jwtUtil.generateRefreshToken("user@sgs.local");

        assertThat(jwtUtil.validateRefreshToken(expired)).isFalse();
        // parseSignedClaims refuse d'office les tokens expirés : le typ ne
        // peut donc pas être lu — isRefreshToken renvoie aussi false. Le
        // token est rejeté par les DEUX chemins, c'est ce qui compte.
        assertThat(jwtUtil.isRefreshToken(expired)).isFalse();
    }

    @Test
    @DisplayName("validateRefreshToken refuse une signature invalide")
    void rejectsTamperedSignature() throws Exception {
        String refresh = jwtUtil.generateRefreshToken("user@sgs.local");

        // Un JwtUtil configuré avec un AUTRE secret rejette le token.
        JwtUtil autreJwtUtil = new JwtUtil();
        var secretField = JwtUtil.class.getDeclaredField("secretKey");
        secretField.setAccessible(true);
        secretField.set(autreJwtUtil, Base64.getEncoder().encodeToString(
                "another-secret-key-totally-different-0123456789abcdef".getBytes()));
        var expField = JwtUtil.class.getDeclaredField("jwtExpiration");
        expField.setAccessible(true);
        expField.set(autreJwtUtil, ACCESS_MS);
        var refreshField = JwtUtil.class.getDeclaredField("refreshExpiration");
        refreshField.setAccessible(true);
        refreshField.set(autreJwtUtil, REFRESH_MS);

        assertThat(autreJwtUtil.validateRefreshToken(refresh)).isFalse();
        assertThat(autreJwtUtil.isRefreshToken(refresh)).isFalse();

        // Le parse brut lève bien l'exception de signature.
        var mauvaiseCle = Keys.hmacShaKeyFor(Base64.getDecoder().decode(Base64.getEncoder()
                .encodeToString("another-secret-key-totally-different-0123456789abcdef".getBytes())));
        assertThatThrownBy(() -> Jwts.parser()
                .verifyWith(mauvaiseCle)
                .build()
                .parseSignedClaims(refresh))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    @DisplayName("isRefreshToken renvoie false sur un token illisible")
    void isRefreshTokenFalseOnGarbage() {
        assertThat(jwtUtil.isRefreshToken("not-a-jwt")).isFalse();
    }

    @Test
    @DisplayName("Un refresh token expiré est refusé par tous les chemins")
    void expiredRefreshTokenRejectedEverywhere() throws Exception {
        var refreshField = JwtUtil.class.getDeclaredField("refreshExpiration");
        refreshField.setAccessible(true);
        refreshField.set(jwtUtil, -1000L);

        String expired = jwtUtil.generateRefreshToken("user@sgs.local");

        assertThat(jwtUtil.validateRefreshToken(expired)).isFalse();
        assertThat(jwtUtil.isRefreshToken(expired)).isFalse();
    }
}
