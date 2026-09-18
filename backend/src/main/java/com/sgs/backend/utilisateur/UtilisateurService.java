package com.sgs.backend.utilisateur;

import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.dto.UtilisateurUpdateDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service Utilisateur — implémente UserDetailsService pour s'intégrer
 * nativement avec Spring Security.
 *
 * Spring Security utilise cette classe pour :
 *   - Charger un utilisateur par son login lors de l'authentification
 *   - Comparer le mot de passe hashé
 *   - Attribuer les rôles/permissions
 */
@Service
@RequiredArgsConstructor
public class UtilisateurService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Appelé par Spring Security (viaJwtAuthFilter) pour charger un utilisateur
     * par son identifiant (login/email).
     *
     * @param login le login de l'utilisateur
     * @return un objet UserDetails que Spring Security utilise pour vérifier
     *         le mot de passe et les rôles
     * @throws UsernameNotFoundException si aucun utilisateur ne correspond
     */
    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Aucun utilisateur trouvé avec le login : " + login
                ));

        // Construire les authorities (rôles) au format Spring Security
        // Le préfixe "ROLE_" est une convention Spring Security pour les rôles
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole().name())
        );

        // Retourner un objet User Spring Security avec :
        //   - login comme username
        //   - motDePasse hashé
        //   - rôles
        //   - isEnabled = utilisateur.actif — un compte désactivé est refusé
        //     au login (DisabledException -> 403) ET à chaque requête (voir
        //     JwtAuthFilter, qui recontrôle isEnabled()).
        //   - les trois flags d'expiration/verrouillage restent true : aucune
        //     politique de péremption de compte ou de mot de passe à ce jour.
        return new User(
                utilisateur.getLogin(),
                utilisateur.getMotDePasse(),
                utilisateur.isActif(), true, true, true,
                authorities
        );
    }

    /**
     * Recherche un utilisateur par son login (pour le controller auth).
     */
    public Utilisateur findByLogin(String login) {
        return utilisateurRepository.findByLogin(login)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur introuvable avec le login : " + login
                ));
    }

    /**
     * Vérifie si un login existe déjà.
     */
    public boolean existsByLogin(String login) {
        return utilisateurRepository.existsByLogin(login);
    }

    /**
     * Crée un nouvel utilisateur avec le mot de passe hashé.
     */
    public Utilisateur create(Utilisateur utilisateur) {
        if (existsByLogin(utilisateur.getLogin())) {
            throw new IllegalArgumentException(
                    "Un utilisateur avec le login '" + utilisateur.getLogin() + "' existe déjà"
            );
        }
        // Hasher le mot de passe avant de sauvegarder
        utilisateur.setMotDePasse(passwordEncoder.encode(utilisateur.getMotDePasse()));
        return utilisateurRepository.save(utilisateur);
    }

    /**
     * Utilisé par UtilisateurController. Pas de CurrentUserService ici
     * (dépendance circulaire : CurrentUserService dépend déjà de ce
     * service pour résoudre "qui est connecté") -- le controller passe
     * directement l'entrepriseId de l'appelant.
     */
    public List<Utilisateur> findAllByEntreprise(Long entrepriseId) {
        return utilisateurRepository.findByEntrepriseId(entrepriseId);
    }

    public Utilisateur findByIdAndEntreprise(Long id, Long entrepriseId) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec id=" + id));
        boolean appartientAuTenant = utilisateur.getEntreprise() != null
                && utilisateur.getEntreprise().getId().equals(entrepriseId);
        if (!appartientAuTenant) {
            throw new ResourceNotFoundException("Utilisateur introuvable avec id=" + id);
        }
        return utilisateur;
    }

    /**
     * Mise à jour d'un utilisateur par un ADMIN de la même entreprise.
     *
     * Deux gardes de sécurité, pensées pour éviter un auto-enfermement :
     *   1. Pas d'action suicidaire sur son propre compte : un ADMIN qui
     *      désactive son compte ou se retire son rôle ADMIN ferme l'accès
     *      admin à tout le monde s'il est seul — refusé systématiquement.
     *   2. Invariant « dernier admin actif » : une opération qui retirerait
     *      son statut d'admin ACTIF au dernier administrateur actif de
     *      l'entreprise est refusée. Via ce endpoint, l'appelant étant lui
     *     -même un admin actif, la garde 1 couvre déjà le cas pratique ;
     *      celle-ci reste un filet pour les évolutions futures (endpoint
     *      de désactivation en masse, changement de @PreAuthorize…).
     */
    public Utilisateur update(Long id, Long entrepriseId, UtilisateurUpdateDTO dto, Utilisateur appelant) {
        Utilisateur utilisateur = findByIdAndEntreprise(id, entrepriseId);

        // Garde 1 : pas d'auto-sabotage du compte appelant.
        if (utilisateur.getId().equals(appelant.getId())) {
            if (!dto.actif()) {
                throw new IllegalArgumentException(
                        "Vous ne pouvez pas désactiver votre propre compte");
            }
            if (appelant.getRole() == UserRole.ADMIN && dto.role() != UserRole.ADMIN) {
                throw new IllegalArgumentException(
                        "Vous ne pouvez pas retirer votre propre rôle ADMIN");
            }
        }

        // Garde 2 : invariant « dernier admin actif » de l'entreprise.
        boolean perteDuStatutAdmin = utilisateur.getRole() == UserRole.ADMIN
                && utilisateur.isActif()
                && (dto.role() != UserRole.ADMIN || !dto.actif());
        if (perteDuStatutAdmin) {
            long adminsActifs = utilisateurRepository
                    .countByEntrepriseIdAndRoleAndActifTrue(entrepriseId, UserRole.ADMIN);
            if (adminsActifs <= 1) {
                throw new IllegalArgumentException(
                        "Impossible : c'est le dernier administrateur actif de l'entreprise");
            }
        }

        utilisateur.setNom(dto.nom());
        utilisateur.setPrenom(dto.prenom());
        utilisateur.setMail(dto.mail());
        utilisateur.setNumTel(dto.numTel());
        utilisateur.setRole(dto.role());
        utilisateur.setActif(dto.actif());
        return utilisateurRepository.save(utilisateur);
    }

    public void delete(Long id, Long entrepriseId) {
        Utilisateur utilisateur = findByIdAndEntreprise(id, entrepriseId);
        utilisateurRepository.delete(utilisateur);
    }
}
