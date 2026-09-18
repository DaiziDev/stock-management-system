package com.sgs.backend.config;

import com.sgs.backend.entreprise.Entreprise;
import com.sgs.backend.entreprise.EntrepriseRepository;

import org.springframework.jdbc.core.JdbcTemplate;
import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.Utilisateur;
import com.sgs.backend.utilisateur.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Exécuté au démarrage de l'application.
 *
 * Crée le compte SUPER_ADMIN "bootstrap" — l'opérateur de la plateforme.
 * C'est lui qui onboardera les entreprises clientes depuis la console
 * plateforme (créer l'entreprise, puis son premier ADMIN). Il n'est
 * rattaché à AUCUNE entreprise : son JWT porte entrepriseId=null, ce qui
 * le distingue structurellement des comptes clients.
 *
 * C'est la réponse au problème soulevé par le flux logique fonctionnel :
 * "le tout premier ADMIN du système ne peut pas être créé par un ADMIN
 * déjà connecté" → tranché par un compte inséré au démarrage.
 *
 * Le mot de passe est hashé dynamiquement avec BCrypt — pas besoin de hash
 * pré-généré en dur dans le SQL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private static final String BOOTSTRAP_LOGIN = "admin@sgs.local";

    private final UtilisateurRepository utilisateurRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        elargirColonnePhotoSiNecessaire();
        upgraderBootstrapEnSuperAdmin();
        creerEntrepriseDemo();
    }

    /**
     * La photo d'article est stockée en base64 (data URL) : une colonne
     * varchar(255) créée par une ancienne version du schéma rejette la
     * donnée ("valeur trop longue pour le type character varying(255)").
     * On l'élargit en TEXT, une seule fois, en interrogeant le schéma réel.
     *
     * NB : information_schema.columns.data_type renvoie le NOM du type
     * PostgreSQL ("character varying"), pas son OID interne — une première
     * version comparait à l'OID 1043 et ne matchait jamais.
     */
    private void elargirColonnePhotoSiNecessaire() {
        try {
            // Hibernate (ddl-auto: update) ne modifie JAMAIS le type d'une
            // colonne existante : c'est à nous de le faire, ici.
            String typeColonne = jdbcTemplate.query(
                    "SELECT data_type FROM information_schema.columns " +
                    "WHERE table_name = 'article' AND column_name = 'photo'",
                    (rs, i) -> rs.getString(1))
                    .stream().findFirst().orElse(null);

            if (typeColonne == null) {
                log.info("ℹ️ Table article absente : la colonne photo sera créée directement en TEXT.");
            } else if ("character varying".equals(typeColonne)) {
                jdbcTemplate.execute("ALTER TABLE article ALTER COLUMN photo TYPE text");
                log.info("✅ Colonne article.photo élargie varchar -> TEXT (photos en base64)");
            } else {
                log.info("ℹ️ Colonne article.photo déjà en {}", typeColonne);
            }
        } catch (Exception ex) {
            // SGBD atypique ou DB momentanément indisponible : ne jamais
            // bloquer le démarrage pour une migration de confort.
            log.warn("Élargissement colonne photo non appliqué : {}", ex.getMessage());
        }
    }

    /**
     * Crée le compte bootstrap s'il n'existe pas, ou le MET À NIVEAU s'il
     * provient d'une base créée avant l'introduction du rôle SUPER_ADMIN
     * (il était alors ADMIN rattaché à l'entreprise de démo — ce qui aurait
     * fini par le traiter comme un simple tenant). Idempotent : relancer
     * l'app ne modifie rien.
     */
    private void upgraderBootstrapEnSuperAdmin() {
        Utilisateur bootstrap = utilisateurRepository.findByLogin(BOOTSTRAP_LOGIN).orElseGet(() -> {
            Utilisateur nouveau = new Utilisateur();
            nouveau.setNom("Plateforme");
            nouveau.setPrenom("Admin");
            nouveau.setLogin(BOOTSTRAP_LOGIN);
            nouveau.setMotDePasse(passwordEncoder.encode("admin123"));
            nouveau.setMail(BOOTSTRAP_LOGIN);
            nouveau.setNumTel("+237 6 00 00 00 00");
            nouveau.setRole(UserRole.SUPER_ADMIN);
            nouveau.setEntreprise(null);
            utilisateurRepository.save(nouveau);
            log.info("✅ Compte SUPER_ADMIN bootstrap créé — login: {} | mdp: admin123", BOOTSTRAP_LOGIN);
            return nouveau;
        });

        if (bootstrap.getRole() != UserRole.SUPER_ADMIN || bootstrap.getEntreprise() != null) {
            bootstrap.setRole(UserRole.SUPER_ADMIN);
            bootstrap.setEntreprise(null);
            utilisateurRepository.save(bootstrap);
            log.info("🔄 Compte {} mis à niveau vers SUPER_ADMIN (rôle/entreprise corrigés)", BOOTSTRAP_LOGIN);
        } else {
            log.info("ℹ️  Compte bootstrap déjà à niveau, pas de modification");
        }
    }

    /**
     * Entreprise de démonstration pour tester le côté "tenant". Ce n'est PAS
     * l'entreprise de la plateforme : le SUPER_ADMIN n'y est plus rattaché,
     * elle est désormais une cliente comme les autres.
     */
    private void creerEntrepriseDemo() {
        boolean existe = entrepriseRepository.findAll().stream()
                .anyMatch(e -> "SGS Demo".equals(e.getNom()));
        if (!existe) {
            Entreprise demo = new Entreprise();
            demo.setNom("SGS Demo");
            demo.setMail("contact@sgs-demo.local");
            demo.setNumTel("+237 6 00 00 00 00");
            entrepriseRepository.save(demo);
            log.info("✅ Entreprise de démonstration créée : SGS Demo");
        }
    }
}
