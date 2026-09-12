package com.sgs.backend.config;

import com.sgs.backend.entreprise.Entreprise;
import com.sgs.backend.entreprise.EntrepriseRepository;
import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.Utilisateur;
import com.sgs.backend.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Exécuté une seule fois au démarrage de l'application.
 *
 * Crée l'admin par défaut s'il n'existe pas encore.
 * Le mot de passe est hashé dynamiquement avec BCrypt —
 * pas besoin de hash pré-généré en dur dans le SQL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Créer l'entreprise par défaut si elle n'existe pas
        Entreprise entreprise = entrepriseRepository.findAll().stream()
                .filter(e -> "SGS Demo".equals(e.getNom()))
                .findFirst()
                .orElseGet(() -> {
                    Entreprise newEntreprise = new Entreprise();
                    newEntreprise.setNom("SGS Demo");
                    newEntreprise.setMail("admin@sgs.local");
                    newEntreprise.setNumTel("+237 6 00 00 00 00");
                    return entrepriseRepository.save(newEntreprise);
                });

        // Créer l'admin par défaut s'il n'existe pas
        if (!utilisateurRepository.existsByLogin("admin@sgs.local")) {
            Utilisateur admin = new Utilisateur();
            admin.setNom("Admin");
            admin.setPrenom("SGS");
            admin.setLogin("admin@sgs.local");
            admin.setMotDePasse(passwordEncoder.encode("admin123"));
            admin.setMail("admin@sgs.local");
            admin.setNumTel("+237 6 00 00 00 00");
            admin.setRole(UserRole.ADMIN);
            admin.setEntreprise(entreprise);

            utilisateurRepository.save(admin);
            log.info("✅ Admin par défaut créé — login: admin@sgs.local | mdp: admin123");
        } else {
            log.info("ℹ️  Admin déjà existant, pas de recréation");
        }
    }
}
