package com.sgs.backend.commandeFournisseur;

import com.sgs.backend.article.Article;
import com.sgs.backend.article.ArticleRepository;
import com.sgs.backend.commande.StatutCommandeFournisseur;
import com.sgs.backend.commandeFournisseur.dto.CommandeFournisseurRequestDTO;
import com.sgs.backend.commandeFournisseur.dto.CommandeFournisseurResponseDTO;
import com.sgs.backend.commandeFournisseur.dto.LigneCommandeFournisseurRequestDTO;
import com.sgs.backend.commandeFournisseur.dto.LigneCommandeFournisseurResponseDTO;
import com.sgs.backend.common.ResourceNotFoundException;
import com.sgs.backend.config.CurrentUserService;
import com.sgs.backend.fournisseur.Fournisseur;
import com.sgs.backend.fournisseur.FournisseurRepository;
import com.sgs.backend.ligneCommandeFournisseur.LigneCommandeFournisseur;
import com.sgs.backend.mvtStk.MvtStkService;
import com.sgs.backend.mvtStk.TypeMouvement;
import com.sgs.backend.notification.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommandeFournisseurService {

    private final CommandeFournisseurRepository commandeFournisseurRepository;
    private final FournisseurRepository fournisseurRepository;
    private final ArticleRepository articleRepository;
    private final MvtStkService mvtStkService;
    private final EmailService emailService;
    private final CurrentUserService currentUserService;

    public List<CommandeFournisseurResponseDTO> findAll() {
        return commandeFournisseurRepository.findByEntrepriseIdOrderByDateCommandeDesc(currentUserService.getEntrepriseId())
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public CommandeFournisseurResponseDTO findById(Long id) {
        return toResponseDTO(getCommandeOrThrow(id));
    }

    @Transactional
    public CommandeFournisseurResponseDTO create(CommandeFournisseurRequestDTO dto) {
        Fournisseur fournisseur = getFournisseurOrThrow(dto.fournisseurId());

        CommandeFournisseur commande = new CommandeFournisseur();
        commande.setDateCommande(Instant.now());
        commande.setStatut(StatutCommandeFournisseur.EN_ATTENTE);
        commande.setFournisseur(fournisseur);
        commande.setEntreprise(currentUserService.getEntrepriseCourante());
        commande.setLignes(construireLignes(dto.lignes(), commande));

        commande.setCode("CF-TMP");
        CommandeFournisseur saved = commandeFournisseurRepository.save(commande);
        saved.setCode(String.format("CF-%06d", saved.getId()));
        saved = commandeFournisseurRepository.save(saved);

        // RG-07 : bon de commande envoyé au fournisseur. Best-effort (voir EmailService).
        emailService.envoyerBonCommandeFournisseur(fournisseur.getMail(), saved.getCode(), fournisseur.getNom());

        return toResponseDTO(saved);
    }

    /**
     * EN_ATTENTE -> RECUE. Génère une entrée de stock par ligne (RG-03).
     * Une commande déjà RECUE ne peut pas l'être une seconde fois -- sinon
     * le stock serait compté deux fois (règle explicite du flux fonctionnel).
     */
    @Transactional
    public CommandeFournisseurResponseDTO receptionner(Long id) {
        CommandeFournisseur commande = getCommandeOrThrow(id);
        if (commande.getStatut() != StatutCommandeFournisseur.EN_ATTENTE) {
            throw new IllegalArgumentException(
                    "Seule une commande EN_ATTENTE peut être réceptionnée (statut actuel : " + commande.getStatut() + ")"
            );
        }

        for (LigneCommandeFournisseur ligne : commande.getLignes()) {
            mvtStkService.enregistrerMouvement(
                    ligne.getArticle(), TypeMouvement.ENTREE, ligne.getQuantite(), null, commande.getCode()
            );
        }

        commande.setStatut(StatutCommandeFournisseur.RECUE);
        return toResponseDTO(commandeFournisseurRepository.save(commande));
    }

    public CommandeFournisseurResponseDTO annuler(Long id) {
        CommandeFournisseur commande = getCommandeOrThrow(id);
        if (commande.getStatut() != StatutCommandeFournisseur.EN_ATTENTE) {
            throw new IllegalArgumentException(
                    "Seule une commande EN_ATTENTE peut être annulée (statut actuel : " + commande.getStatut() + ")"
            );
        }
        commande.setStatut(StatutCommandeFournisseur.ANNULEE);
        return toResponseDTO(commandeFournisseurRepository.save(commande));
    }

    // --- Helpers privés ---

    private List<LigneCommandeFournisseur> construireLignes(List<LigneCommandeFournisseurRequestDTO> lignesDto, CommandeFournisseur commande) {
        List<LigneCommandeFournisseur> lignes = new ArrayList<>();
        for (LigneCommandeFournisseurRequestDTO ligneDto : lignesDto) {
            Article article = getArticleOrThrow(ligneDto.articleId());
            LigneCommandeFournisseur ligne = new LigneCommandeFournisseur();
            ligne.setArticle(article);
            ligne.setQuantite(ligneDto.quantite());
            ligne.setPrixUnitaire(article.getPrixUnitaireHt());
            ligne.setCommandeFournisseur(commande);
            lignes.add(ligne);
        }
        return lignes;
    }

    private Fournisseur getFournisseurOrThrow(Long id) {
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur introuvable avec id=" + id));
        Long entrepriseId = currentUserService.getEntrepriseId();
        boolean appartientAuTenant = fournisseur.getEntreprise() != null
                && fournisseur.getEntreprise().getId().equals(entrepriseId);
        if (!appartientAuTenant) {
            throw new ResourceNotFoundException("Fournisseur introuvable avec id=" + id);
        }
        return fournisseur;
    }

    private Article getArticleOrThrow(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable avec id=" + id));
        Long entrepriseId = currentUserService.getEntrepriseId();
        boolean appartientAuTenant = article.getEntreprise() != null
                && article.getEntreprise().getId().equals(entrepriseId);
        if (!appartientAuTenant) {
            throw new ResourceNotFoundException("Article introuvable avec id=" + id);
        }
        return article;
    }

    private CommandeFournisseur getCommandeOrThrow(Long id) {
        CommandeFournisseur commande = commandeFournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande fournisseur introuvable avec id=" + id));
        Long entrepriseId = currentUserService.getEntrepriseId();
        boolean appartientAuTenant = commande.getEntreprise() != null
                && commande.getEntreprise().getId().equals(entrepriseId);
        if (!appartientAuTenant) {
            throw new ResourceNotFoundException("Commande fournisseur introuvable avec id=" + id);
        }
        return commande;
    }

    private CommandeFournisseurResponseDTO toResponseDTO(CommandeFournisseur commande) {
        List<LigneCommandeFournisseurResponseDTO> lignes = commande.getLignes().stream()
                .map(l -> new LigneCommandeFournisseurResponseDTO(
                        l.getId(),
                        l.getArticle().getId(),
                        l.getArticle().getDesignation(),
                        l.getQuantite(),
                        l.getPrixUnitaire(),
                        l.getPrixUnitaire().multiply(BigDecimal.valueOf(l.getQuantite()))
                ))
                .toList();

        BigDecimal total = lignes.stream()
                .map(LigneCommandeFournisseurResponseDTO::sousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CommandeFournisseurResponseDTO(
                commande.getId(),
                commande.getCode(),
                commande.getDateCommande(),
                commande.getStatut(),
                commande.getFournisseur().getId(),
                commande.getFournisseur().getNom(),
                lignes,
                total
        );
    }
}
