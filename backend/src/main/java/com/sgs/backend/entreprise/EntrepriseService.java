package com.sgs.backend.entreprise;

import com.sgs.backend.adresse.Adresse;
import com.sgs.backend.common.DuplicateResourceException;
import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.entreprise.dto.AdminRegisterDTO;
import com.sgs.backend.entreprise.dto.EntrepriseRegisterRequest;
import com.sgs.backend.entreprise.dto.EntrepriseRegisterResponse;
import com.sgs.backend.entreprise.dto.EntrepriseRequestDTO;
import com.sgs.backend.entreprise.dto.EntrepriseResponseDTO;
import com.sgs.backend.roles.UserRole;
import com.sgs.backend.utilisateur.Utilisateur;
import com.sgs.backend.utilisateur.UtilisateurRepository;
import com.sgs.backend.utilisateur.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EntrepriseService {

    private final EntrepriseRepository entrepriseRepository;
    private final UtilisateurService utilisateurService;
    private final UtilisateurRepository utilisateurRepository;

    public List<EntrepriseResponseDTO> findAll() {
        return entrepriseRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public EntrepriseResponseDTO findById(Long id) {
        return toResponseDTO(getOrThrow(id));
    }

    public EntrepriseResponseDTO create(EntrepriseRequestDTO dto) {
        if (entrepriseRepository.existsByNom(dto.nom())) {
            throw new IllegalArgumentException("Une entreprise avec le nom '" + dto.nom() + "' existe déjà");
        }
        Entreprise entreprise = new Entreprise();
        applyDto(entreprise, dto);
        return toResponseDTO(entrepriseRepository.save(entreprise));
    }

    /**
     * Enregistrement PUBLIC d'une entreprise + création atomique de son admin.
     *
     * C'est l'endpoint d'onboarding SaaS : une seule requête anonyme crée le
     * tenant ET le compte qui l'administrera — l'admin naît avec l'entreprise.
     *
     * Garde-fous, dans l'ordre :
     *   1. Unicité du nom d'entreprise (409) — vérifiée ici, doublée du
     *      unique=true en base qui reste le dernier rempart (race condition).
     *   2. Unicité du login/email de l'admin (409) — via UtilisateurService.
     *   3. Rôle forcé à ADMIN côté serveur, JAMAIS lu du client : l'endpoint
     *      étant public, un rôle venu du payload permettrait de créer un
     *      compte de n'importe quel niveau sans contrôle.
     *   4. Mot de passe hashé BCrypt par UtilisateurService.create (jamais
     *      stocké en clair, jamais renvoyé).
     *
     * @Transactional : si la création de l'admin échoue (login pris entre-temps,
     * violation DB...), l'entreprise créée dans la même transaction est
     * ROLLBACKée — jamais d'entreprise orpheline sans admin, qui serait
     * impossible à reconnecter et à supprimer proprement depuis l'API.
     */
    @Transactional
    public EntrepriseRegisterResponse register(EntrepriseRegisterRequest request) {
        if (entrepriseRepository.existsByNom(request.nom())) {
            throw new DuplicateResourceException(
                    "Une entreprise avec le nom '" + request.nom() + "' existe déjà");
        }

        AdminRegisterDTO admin = request.admin();

        // L'email de l'admin sert de login (décision produit) — unicité vérifiée
        // ici pour un 409 clair ; UtilisateurService.create re-vérifie (400) en
        // cas de course entre deux requêtes simultanées.
        if (utilisateurService.existsByLogin(admin.email())) {
            throw new DuplicateResourceException(
                    "Un compte avec l'email '" + admin.email() + "' existe déjà");
        }

        // 1. L'entreprise d'abord (l'utilisateur va référencer sa FK).
        Entreprise entreprise = new Entreprise();
        entreprise.setNom(request.nom());
        entreprise.setMail(request.mail());
        entreprise.setNumTel(request.numTel());
        entreprise.setAdresse(new Adresse(
                request.adresse1(), request.adresse2(), request.ville(),
                request.codePostal(), request.pays()));
        entreprise = entrepriseRepository.save(entreprise);

        // 2. L'admin, rattaché à l'entreprise fraîchement créée. Rôle et état
        //    actif forcés ici (jamais lus du client) ; le hash BCrypt est fait
        //    par create(). Toute exception → rollback de l'entreprise aussi.
        Utilisateur adminUser = new Utilisateur();
        adminUser.setNom(admin.nom());
        adminUser.setPrenom(admin.prenom());
        adminUser.setLogin(admin.email());
        adminUser.setMotDePasse(admin.motDePasse());
        adminUser.setMail(admin.email());
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEntreprise(entreprise);

        utilisateurService.create(adminUser);

        return new EntrepriseRegisterResponse(entreprise.getId(), entreprise.getNom(), adminUser.getLogin());
    }

    public EntrepriseResponseDTO update(Long id, EntrepriseRequestDTO dto) {
        Entreprise entreprise = getOrThrow(id);
        applyDto(entreprise, dto);
        return toResponseDTO(entrepriseRepository.save(entreprise));
    }

    /**
     * Suppression de l'entreprise (DELETE /api/entreprises/me) — self-destruct.
     *
     * Deux étapes, une seule transaction :
     *   1. Suppression BULK de tous les comptes utilisateurs du tenant
     *      (l'appelant inclus — son token meurt avec l'entreprise).
     *   2. Suppression BULK de l'entreprise elle-même.
     *
     * Pourquoi BULK (JPQL) et pas deleteById/deleteAll ? Voir le commentaire
     * de EntrepriseRepository.deleteByIdBulk : un em.remove() passe l'entité
     * en statut DELETED, casse contains(), et fait planter le flush du commit
     * sur les AUTRES entités managées qui la référencent encore (l'Utilisateur
     * chargé par CurrentUserService sous open-in-view) — TransientObjectException
     * → 403 brut. Le JPQL ne touche pas au contexte de persistance.
     *
     * Sémantique retenue : les COMPTES partent avec l'entreprise (une société
     * dissoute n'a plus d'employés), mais les DONNÉES MÉTIER bloquent — s'il
     * reste articles/clients/ventes, la contrainte FK refuse le DELETE SQL de
     * l'étape 2 → DataIntegrityViolationException → 409 propre (purge métier
     * à faire d'abord, volontairement manuelle et explicite).
     */
    @Transactional
    public void delete(Long id) {
        if (!entrepriseRepository.existsById(id)) {
            throw new ResourceNotFoundException("Entreprise introuvable avec id=" + id);
        }
        utilisateurRepository.deleteByEntrepriseIdBulk(id);
        entrepriseRepository.deleteByIdBulk(id);
    }

    private Entreprise getOrThrow(Long id) {
        return entrepriseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise introuvable avec id=" + id));
    }

    private void applyDto(Entreprise entreprise, EntrepriseRequestDTO dto) {
        entreprise.setNom(dto.nom());
        entreprise.setMail(dto.mail());
        entreprise.setNumTel(dto.numTel());
        entreprise.setAdresse(new Adresse(dto.adresse1(), dto.adresse2(), dto.ville(), dto.codePostal(), dto.pays()));
    }

    private EntrepriseResponseDTO toResponseDTO(Entreprise entreprise) {
        Adresse adresse = entreprise.getAdresse();
        return new EntrepriseResponseDTO(
                entreprise.getId(),
                entreprise.getNom(),
                adresse != null ? adresse.getAdresse1() : null,
                adresse != null ? adresse.getAdresse2() : null,
                adresse != null ? adresse.getVille() : null,
                adresse != null ? adresse.getCodePostal() : null,
                adresse != null ? adresse.getPays() : null,
                entreprise.getMail(),
                entreprise.getNumTel()
        );
    }
}
