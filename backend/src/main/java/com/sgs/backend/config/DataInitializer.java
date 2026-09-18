package com.sgs.backend.config;

import com.sgs.backend.entreprise.Entreprise;
import com.sgs.backend.entreprise.EntrepriseRepository;
import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.Utilisateur;
import com.sgs.backend.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Exécuté une seule fois au démarrage de l'application.
 *
 * Crée l'admin par défaut s'il n'existe pas encore.
 * Le mot de passe est hashé dynamiquement avec BCrypt —
 * pas besoin de hash pré-généré en dur dans le SQL.
 *
 * ⚠️ Plus aucun identifiant par défaut en dur dans le code :
 *   - ADMIN_LOGIN     : login du compte bootstrap (défaut : admin@sgs.local)
 *   - ADMIN_PASSWORD  : mot de passe du compte bootstrap — REQUIS, sans valeur
 *                       par défaut. L'application refuse de démarrer sans.
 *                       Générer un mot de passe fort : openssl rand -base64 24
 *   - ADMIN_ROLE      : rôle du compte bootstrap (défaut : ADMIN)
 *
 * L'entreprise de démonstration « SGS Demo » n'est créée que si aucune
 * entreprise n'existe encore (premier démarrage en dev) — jamais recréée
 * après suppression, pour éviter une donnée fantome en production.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin-login:admin@sgs.local}")
    private String adminLogin;

    /** Requis : pas de défaut committé (voir application.yaml). */
    @Value("${app.bootstrap.admin-password}")
    private String adminPassword;

    @Value("${app.bootstrap.admin-role:ADMIN}")
    private String adminRole;

    @Override
    public void run(String... args) {
        // Créer l'entreprise par défaut si (et seulement si) la base est vide
        Entreprise entreprise = entrepriseRepository.findAll().stream()
                .findFirst()
                .orElseGet(() -> {
                    Entreprise newEntreprise = new Entreprise();
                    newEntreprise.setNom("SGS Demo");
                    newEntreprise.setMail("admin@sgs.local");
                    newEntreprise.setNumTel("+237 6 00 00 00 00");
                    Entreprise saved = entrepriseRepository.save(newEntreprise);
                    log.info("🏢 Entreprise de démonstration créée (SGS Demo)");
                    return saved;
                });

        // Créer l'admin bootstrap s'il n'existe pas
        if (!utilisateurRepository.existsByLogin(adminLogin)) {
            Utilisateur admin = new Utilisateur();
            admin.setNom("Admin");
            admin.setPrenom("SGS");
            admin.setLogin(adminLogin);
            admin.setMotDePasse(passwordEncoder.encode(adminPassword));
            admin.setMail(adminLogin);
            admin.setNumTel("+237 6 00 00 00 00");
            admin.setRole(UserRole.valueOf(adminRole));
            admin.setEntreprise(entreprise);

            utilisateurRepository.save(admin);
            log.info("✅ Admin bootstrap créé — login: {} (mot de passe issu de ADMIN_PASSWORD)", adminLogin);
        } else {
            log.info("ℹ️  Admin bootstrap déjà existant, pas de recréation");
        }
    }
}
