package com.sgs.backend.utilisateur;

import com.sgs.backend.config.JwtAuthFilter;
import com.sgs.backend.config.JwtUtil;
import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.dto.UtilisateurUpdateDTO;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Couvre la chaîne « compte désactivé » de bout en bout, sans base :
 *   - loadUserByUsername mappe actif -> isEnabled() (UserDetails Spring)
 *   - JwtAuthFilter refuse d'authentifier un token valide d'un compte désactivé
 *   - UtilisateurService.update refuse l'auto-désactivation et la
 *     désactivation/rétrogradation du dernier admin actif
 *
 * NB : JwtUtil n'est PAS mocké — Mockito (via Byte Buddy) ne peut pas mocker
 * les classes concrètes sur JDK 25. Un instance réelle est construite et
 * configurée via ReflectionTestUtils (les @Value ne s'appliquent pas hors
 * conteneur) : plus fidèle au comportement réel qu'un mock.
 */
@ExtendWith(MockitoExtension.class)
class UtilisateurServiceActifTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserDetailsService userDetailsService;

    /** Vrai JwtUtil (mock impossible sur JDK 25) : clé Base64 de 48 octets. */
    private JwtUtil jwtUtil;

    /** Clé de test : "backend-test-secret-key-for-jwt-signing-2026" en Base64. */
    private static final String TEST_SECRET =
            "YmFja2VuZC10ZXN0LXNlY3JldC1rZXktZm9yLWp3dC1zaWduaW5nLTIwMjY=";

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpiration", 3_600_000L);
    }

    // ───────────── Helpers ─────────────

    private UtilisateurService service() {
        return new UtilisateurService(utilisateurRepository, passwordEncoder);
    }

    private Utilisateur user(long id, String login, UserRole role, boolean actif) {
        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setLogin(login);
        u.setRole(role);
        u.setActif(actif);
        return u;
    }

    private UtilisateurUpdateDTO dto(UserRole role, Boolean actif) {
        return new UtilisateurUpdateDTO("Nom", "Prenom", "mail@x.io", "+237", role, actif);
    }

    private JwtAuthFilter filterWithMocks() {
        return new JwtAuthFilter(jwtUtil, userDetailsService);
    }

    // eq()/any() restent utilisés par les gardes du service ci-dessous.

    // ───────────── Mapping actif -> isEnabled ─────────────

    @Test
    @DisplayName("loadUserByUsername : isEnabled() reflète utilisateur.actif")
    void loadUserMapsActifToEnabled() {
        when(utilisateurRepository.findByLogin("vendeur@x.io"))
                .thenReturn(Optional.of(user(2, "vendeur@x.io", UserRole.VENDEUR, false)));

        UserDetails details = service().loadUserByUsername("vendeur@x.io");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("loadUserByUsername : un compte actif reste enabled")
    void loadUserMapsActifTrueToEnabled() {
        when(utilisateurRepository.findByLogin("admin@x.io"))
                .thenReturn(Optional.of(user(1, "admin@x.io", UserRole.ADMIN, true)));

        UserDetails details = service().loadUserByUsername("admin@x.io");

        assertThat(details.isEnabled()).isTrue();
    }

    // ───────────── JwtAuthFilter x compte désactivé ─────────────

    @Test
    @DisplayName("JwtAuthFilter : token valide + compte désactivé → SecurityContext vide")
    void filterDoesNotAuthenticateDisabledAccount() throws Exception {
        UserDetails disabled = org.springframework.security.core.userdetails.User
                .withUsername("desactive@x.io")
                .password("hash")
                .roles("VENDEUR")
                .disabled(true)
                .build();

        // Vrai token signé par le vrai JwtUtil — pas de mock du cycle token.
        String token = jwtUtil.generateToken("desactive@x.io", "VENDEUR", 1L);
        when(userDetailsService.loadUserByUsername("desactive@x.io")).thenReturn(disabled);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filterWithMocks().doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("JwtAuthFilter : token valide + compte actif → authentification posée")
    void filterAuthenticatesEnabledAccount() throws Exception {
        UserDetails active = org.springframework.security.core.userdetails.User
                .withUsername("actif@x.io")
                .password("hash")
                .roles("VENDEUR")
                .build();

        String token = jwtUtil.generateToken("actif@x.io", "VENDEUR", 1L);
        when(userDetailsService.loadUserByUsername("actif@x.io")).thenReturn(active);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filterWithMocks().doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        verify(chain, times(1)).doFilter(request, response);
    }

    // ───────────── Gardes du service ─────────────

    @Test
    @DisplayName("update : la désactivation de son propre compte est refusée")
    void updateRejectsSelfDisable() {
        Utilisateur appelant = user(1, "admin@x.io", UserRole.ADMIN, true);
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(appelant));

        assertThatThrownBy(() ->
                service().update(1L, 10L, dto(UserRole.ADMIN, false), appelant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("désactiver votre propre compte");
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    @DisplayName("update : un admin ne peut pas se retirer son propre rôle ADMIN")
    void updateRejectsSelfDemotion() {
        Utilisateur appelant = user(1, "admin@x.io", UserRole.ADMIN, true);
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(appelant));

        assertThatThrownBy(() ->
                service().update(1L, 10L, dto(UserRole.VENDEUR, true), appelant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("retirer votre propre rôle ADMIN");
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    @DisplayName("update : la désactivation du dernier admin actif est refusée")
    void updateRejectsDisablingLastActiveAdmin() {
        Utilisateur appelant = user(1, "admin@x.io", UserRole.ADMIN, true);
        Utilisateur cible = user(2, "admin2@x.io", UserRole.ADMIN, true);
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(cible));
        when(utilisateurRepository.countByEntrepriseIdAndRoleAndActifTrue(10L, UserRole.ADMIN))
                .thenReturn(1L);

        assertThatThrownBy(() ->
                service().update(2L, 10L, dto(UserRole.ADMIN, false), appelant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dernier administrateur actif");
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    @DisplayName("update : la rétrogradation du dernier admin actif est refusée")
    void updateRejectsDemotingLastActiveAdmin() {
        Utilisateur appelant = user(1, "admin@x.io", UserRole.ADMIN, true);
        Utilisateur cible = user(2, "admin2@x.io", UserRole.ADMIN, true);
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(cible));
        when(utilisateurRepository.countByEntrepriseIdAndRoleAndActifTrue(10L, UserRole.ADMIN))
                .thenReturn(1L);

        assertThatThrownBy(() ->
                service().update(2L, 10L, dto(UserRole.VENDEUR, true), appelant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dernier administrateur actif");
    }

    @Test
    @DisplayName("update : désactiver un non-admin ne consulte pas le compteur d'admins")
    void updateOfNonAdminSkipsAdminCounter() {
        Utilisateur appelant = user(1, "admin@x.io", UserRole.ADMIN, true);
        Utilisateur cible = user(3, "vendeur@x.io", UserRole.VENDEUR, true);
        when(utilisateurRepository.findById(3L)).thenReturn(Optional.of(cible));

        service().update(3L, 10L, dto(UserRole.VENDEUR, false), appelant);

        verify(utilisateurRepository, never())
                .countByEntrepriseIdAndRoleAndActifTrue(any(), any());
        verify(utilisateurRepository).save(cible);
    }

    @Test
    @DisplayName("update : au moins 2 admins actifs → la désactivation passe")
    void updateAllowsDisablingAdminWhenTwoActiveAdmins() {
        Utilisateur appelant = user(1, "admin@x.io", UserRole.ADMIN, true);
        Utilisateur cible = user(2, "admin2@x.io", UserRole.ADMIN, true);
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(cible));
        when(utilisateurRepository.countByEntrepriseIdAndRoleAndActifTrue(10L, UserRole.ADMIN))
                .thenReturn(2L);

        service().update(2L, 10L, dto(UserRole.ADMIN, false), appelant);

        verify(utilisateurRepository).save(cible);
        assertThat(cible.isActif()).isFalse();
    }
}
