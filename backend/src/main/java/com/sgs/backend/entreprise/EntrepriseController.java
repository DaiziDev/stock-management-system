package com.sgs.backend.entreprise;

import com.sgs.backend.entreprise.dto.EntrepriseRequestDTO;
import com.sgs.backend.entreprise.dto.EntrepriseResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entreprises")
@RequiredArgsConstructor
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    @GetMapping
    public ResponseEntity<List<EntrepriseResponseDTO>> findAll() {
        return ResponseEntity.ok(entrepriseService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntrepriseResponseDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(entrepriseService.findById(id));
    }

    @PostMapping
    public ResponseEntity<EntrepriseResponseDTO> create(@Valid @RequestBody EntrepriseRequestDTO dto) {
        EntrepriseResponseDTO created = entrepriseService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntrepriseResponseDTO> update(@PathVariable Long id, @Valid @RequestBody EntrepriseRequestDTO dto) {
        return ResponseEntity.ok(entrepriseService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        entrepriseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
