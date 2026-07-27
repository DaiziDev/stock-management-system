package com.sgs.backend.categorie;

import com.sgs.backend.categorie.dto.CategorieRequestDTO;
import com.sgs.backend.categorie.dto.CategorieResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Le Controller ne fait QUE : recevoir la requête HTTP, appeler le Service,
// renvoyer une réponse HTTP avec le bon code de statut. Aucune logique
// métier ici -- si tu as un "if" métier dans un controller, il est mal placé.
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategorieController {

    private final CategorieService categorieService;

    @GetMapping
    public ResponseEntity<List<CategorieResponseDTO>> findAll() {
        return ResponseEntity.ok(categorieService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategorieResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(categorieService.findById(id));
    }

    @PostMapping
    public ResponseEntity<CategorieResponseDTO> create(@Valid @RequestBody CategorieRequestDTO dto) {
        CategorieResponseDTO created = categorieService.create(dto);
        // 201 Created, pas 200 : on vient de créer une ressource.
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategorieResponseDTO> update(@PathVariable Long id, @Valid @RequestBody CategorieRequestDTO dto) {
        return ResponseEntity.ok(categorieService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categorieService.delete(id);
        // 204 No Content : succès, mais rien à renvoyer dans le corps.
        return ResponseEntity.noContent().build();
    }
}
