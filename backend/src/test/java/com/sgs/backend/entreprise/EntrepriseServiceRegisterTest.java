package com.sgs.backend.entreprise;

import com.sgs.backend.common.DuplicateResourceException;
import com.sgs.backend.entreprise.dto.AdminRegisterDTO;
import com.sgs.backend.entreprise.dto.EntrepriseRegisterRequest;
import com.sgs.backend.entreprise.dto.EntrepriseRegisterResponse;
import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.Utilisateur;
import com.sgs.backend.utilisateur.UtilisateurRepository;
import com.sgs.backend.utilisateur.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires du register() public — l'onboarding SaaS.
 *
 * Sans base : le contrat du service est vérifié via les mocks des
 * REPOSITORIES (interfaces — mockables sur JDK 25). UtilisateurService est
 * instancié RÉEL (Mockito ne peut pas mocker une classe concrète sur JDK 25,
 * cf. UtilisateurServiceActifTest) : cela permet de vérifier en vrai que le
 * mot de passe ressort bien HASHÉ de UtilisateurService.create.
 *
 * La vérification en base (contraintes uniques) et le rollback transactionnel
 * sont couverts par le test E2E curl sur instance réelle (plan Phase 3).
 */
@ExtendWith(MockitoExtension.class)
class EntrepriseServiceRegisterTest {

    @Mock
    private EntrepriseRepository entrepriseRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    /** Clé : le service testé porte le VRAI UtilisateurService (pas un mock). */
    private UtilisateurService utilisateurService;

    @BeforeEach
    void setUp() {
        utilisateurService = new UtilisateurService(utilisateurRepository, passwordEncoder);

        // Par défaut, aucun doublon : nom et login libres.
        lenient().when(entrepriseRepository.existsByNom(any())).thenReturn(false);
        lenient().when(utilisateurRepository.existsByLogin(any())).thenReturn(false);
        lenient().when(entrepriseRepository.save(any())).thenAnswer(inv -> {
            Entreprise e = inv.getArgument(0);
            e.setId(42L); // simule l'auto-génération d'id
            return e;
        });
        lenient().when(utilisateurRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // BCrypt réel trop lent pour des tests unitaires : stub déterministe.
        lenient().when(passwordEncoder.encode(any())).thenReturn("$2a$hashed");
    }

    // ───────────── Helpers ─────────────

    private EntrepriseService service() {
        return new EntrepriseService(entrepriseRepository, utilisateurService, utilisateurRepository);
    }

    private EntrepriseRegisterRequest request() {
        return new EntrepriseRegisterRequest(
                "Sosucam", "contact@sosucam.cm", "+237 6 55 00 00 00",
                "Rue 1.234", null, "Douala", null, "Cameroun",
                new AdminRegisterDTO("Sow", "Armand", "armand@sosucam.cm", "Password123")
        );
    }

    // ───────────── Cas nominal ─────────────

    @Test
    @DisplayName("register : crée l'entreprise + l'admin, retourne id/nom/login")
    void registerCreatesEntrepriseAndAdmin() {
        EntrepriseRegisterResponse response = service().register(request());

        assertThat(response.entrepriseId()).isEqualTo(42L);
        assertThat(response.nom()).isEqualTo("Sosucam");
        assertThat(response.adminLogin()).isEqualTo("armand@sosucam.cm");
    }

    @Test
    @DisplayName("register : le rôle de l'admin est forcé à ADMIN (jamais lu du client)")
    void registerForcesAdminRole() {
        service().register(request());

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("register : le mot de passe ressort HASHÉ (BCrypt via UtilisateurService.create)")
    void registerStoresHashedPassword() {
        service().register(request());

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        // Le hash est fait par create() : le mot de passe stocké n'est
        // ni le brut, ni null.
        assertThat(captor.getValue().getMotDePasse()).isEqualTo("$2a$hashed");
        assertThat(captor.getValue().getMotDePasse()).isNotEqualTo("Password123");

        // La réponse ne contient JAMAIS le mot de passe.
        EntrepriseRegisterResponse response = service().register(request());
        assertThat(response.toString()).doesNotContain("Password123");
    }

    @Test
    @DisplayName("register : l'admin est rattaché à l'entreprise fraîchement créée")
    void registerLinksAdminToNewEntreprise() {
        service().register(request());

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());
        Utilisateur admin = captor.getValue();

        assertThat(admin.getEntreprise()).isNotNull();
        assertThat(admin.getEntreprise().getNom()).isEqualTo("Sosucam");
        assertThat(admin.getLogin()).isEqualTo("armand@sosucam.cm");
        assertThat(admin.getMail()).isEqualTo("armand@sosucam.cm");
    }

    // ───────────── Refus métier (409) ─────────────

    @Test
    @DisplayName("register : nom d'entreprise déjà pris → DuplicateResourceException, aucune écriture")
    void registerRejectsDuplicateNom() {
        when(entrepriseRepository.existsByNom("Sosucam")).thenReturn(true);

        assertThatThrownBy(() -> service().register(request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Sosucam");

        verify(entrepriseRepository, never()).save(any());
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    @DisplayName("register : email admin déjà pris → DuplicateResourceException, aucune écriture")
    void registerRejectsDuplicateLogin() {
        when(utilisateurRepository.existsByLogin("armand@sosucam.cm")).thenReturn(true);

        assertThatThrownBy(() -> service().register(request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("armand@sosucam.cm");

        verify(entrepriseRepository, never()).save(any());
        verify(utilisateurRepository, never()).save(any());
    }

    // ───────────── Cohérence des garde-fous ─────────────

    @Test
    @DisplayName("register : le nom est vérifié AVANT le login (message le plus actionnable d'abord)")
    void registerChecksNomBeforeLogin() {
        when(entrepriseRepository.existsByNom(any())).thenReturn(true);
        // lenient : jamais atteint (le contrôle du nom lève avant) — c'est
        // justement l'ordre qu'on vérifie ici.
        lenient().when(utilisateurRepository.existsByLogin(any())).thenReturn(true);

        assertThatThrownBy(() -> service().register(request()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("entreprise");
    }
}
