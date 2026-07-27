package com.sgs.backend.article;

import com.sgs.backend.article.dto.ArticleRequestDTO;
import com.sgs.backend.article.dto.ArticleResponseDTO;
import com.sgs.backend.categorie.Categorie;
import com.sgs.backend.categorie.CategorieRepository;
import com.sgs.backend.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    // On injecte aussi CategorieRepository ici : c'est ce Service qui a besoin
    // d'aller vérifier que la catégorie envoyée par id existe réellement.
    // Ce n'est PAS le rôle du Controller (routage HTTP) ni celui du
    // Repository Article (accès aux données Article uniquement).
    private final CategorieRepository categorieRepository;

    public List<ArticleResponseDTO> findAll() {
        return articleRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public ArticleResponseDTO findById(Long id) {
        Article article = getArticleOrThrow(id);
        return toResponseDTO(article);
    }

    public ArticleResponseDTO create(ArticleRequestDTO dto) {
        if (articleRepository.existsByCodeArticle(dto.codeArticle())) {
            throw new IllegalArgumentException("Un article avec le code '" + dto.codeArticle() + "' existe déjà");
        }
        Categorie categorie = getCategorieOrThrow(dto.categorieId());

        Article article = new Article();
        applyDto(article, dto, categorie);

        Article saved = articleRepository.save(article);
        return toResponseDTO(saved);
    }

    public ArticleResponseDTO update(Long id, ArticleRequestDTO dto) {
        Article article = getArticleOrThrow(id);
        Categorie categorie = getCategorieOrThrow(dto.categorieId());

        applyDto(article, dto, categorie);

        Article saved = articleRepository.save(article);
        return toResponseDTO(saved);
    }

    public void delete(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Article introuvable avec id=" + id);
        }
        articleRepository.deleteById(id);
    }

    // --- Helpers privés ---

    private Article getArticleOrThrow(Long id) {
        return articleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article introuvable avec id=" + id));
    }

    private Categorie getCategorieOrThrow(Long categorieId) {
        return categorieRepository.findById(categorieId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec id=" + categorieId));
    }

    private void applyDto(Article article, ArticleRequestDTO dto, Categorie categorie) {
        article.setCodeArticle(dto.codeArticle());
        article.setDesignation(dto.designation());
        article.setPrixUnitaireHt(dto.prixUnitaireHt());
        article.setTauxTva(dto.tauxTva());
        // Le TTC n'est jamais reçu du client : on le calcule nous-mêmes.
        // Règle : c'est une info dérivée, pas une info saisie -- si on
        // laissait le frontend l'envoyer, rien n'empêcherait un TTC
        // incohérent avec le HT et la TVA envoyés à côté.
        article.setPrixUnitaireTtc(calculerTtc(dto.prixUnitaireHt(), dto.tauxTva()));
        article.setPhoto(dto.photo());
        article.setCategorie(categorie);
    }

    private BigDecimal calculerTtc(BigDecimal prixHt, BigDecimal tauxTva) {
        // TTC = HT * (1 + taux/100), taux exprimé en pourcentage (ex: 19.25)
        BigDecimal coefficient = BigDecimal.ONE.add(tauxTva.divide(BigDecimal.valueOf(100)));
        return prixHt.multiply(coefficient).setScale(2, RoundingMode.HALF_UP);
    }

    private ArticleResponseDTO toResponseDTO(Article article) {
        ArticleResponseDTO.CategorieSummaryDTO categorieDto = article.getCategorie() != null
                ? new ArticleResponseDTO.CategorieSummaryDTO(
                        article.getCategorie().getId(),
                        article.getCategorie().getDesignation())
                : null;

        return new ArticleResponseDTO(
                article.getId(),
                article.getCodeArticle(),
                article.getDesignation(),
                article.getPrixUnitaireHt(),
                article.getTauxTva(),
                article.getPrixUnitaireTtc(),
                article.getPhoto(),
                categorieDto
        );
    }
}
