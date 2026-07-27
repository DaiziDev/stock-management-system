package com.sgs.backend.categorie;

import com.sgs.backend.categorie.dto.CategorieRequestDTO;
import com.sgs.backend.categorie.dto.CategorieResponseDTO;
import com.sgs.backend.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

// @Service : cette classe contient la LOGIQUE MÉTIER. C'est elle qui décide
// des règles ("un code de catégorie doit être unique"), pas le Controller
// (qui ne fait que router la requête HTTP) ni le Repository (qui ne fait
// que parler à la base de données).
//
// @RequiredArgsConstructor (Lombok) génère un constructeur avec tous les
// champs "final" -> c'est comme ça qu'on fait de l'injection de dépendances
// par constructeur, la manière recommandée avec Spring (plutôt que @Autowired
// sur le champ directement).
@Service
@RequiredArgsConstructor
public class CategorieService {

    private final CategorieRepository categorieRepository;

    public List<CategorieResponseDTO> findAll() {
        return categorieRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    public CategorieResponseDTO findById(Long id) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec id=" + id));
        return toResponseDTO(categorie);
    }

    public CategorieResponseDTO create(CategorieRequestDTO dto) {
        if (categorieRepository.existsByCode(dto.code())) {
            // Règle métier simple : pas deux catégories avec le même code.
            // C'est le genre de règle qui n'a rien à faire dans le Controller.
            throw new IllegalArgumentException("Une catégorie avec le code '" + dto.code() + "' existe déjà");
        }
        Categorie categorie = new Categorie();
        categorie.setCode(dto.code());
        categorie.setDesignation(dto.designation());
        Categorie saved = categorieRepository.save(categorie);
        return toResponseDTO(saved);
    }

    public CategorieResponseDTO update(Long id, CategorieRequestDTO dto) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable avec id=" + id));
        categorie.setCode(dto.code());
        categorie.setDesignation(dto.designation());
        Categorie saved = categorieRepository.save(categorie);
        return toResponseDTO(saved);
    }

    public void delete(Long id) {
        if (!categorieRepository.existsById(id)) {
            throw new ResourceNotFoundException("Catégorie introuvable avec id=" + id);
        }
        categorieRepository.deleteById(id);
    }

    // Petite méthode privée de mapping Entité -> DTO. Sur un projet plus gros
    // on utiliserait MapStruct pour générer ce mapping, mais à la main
    // c'est plus simple à comprendre pour l'instant.
    private CategorieResponseDTO toResponseDTO(Categorie categorie) {
        return new CategorieResponseDTO(categorie.getId(), categorie.getCode(), categorie.getDesignation());
    }
}
