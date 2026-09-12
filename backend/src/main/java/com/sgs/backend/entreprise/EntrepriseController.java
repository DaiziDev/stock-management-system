package com.sgs.backend.entreprise;

import com.sgs.backend.entreprise.dto.EntrepriseRequestDTO;
import com.sgs.backend.entreprise.dto.EntrepriseResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/entreprises")
@RequiredArgsConstructor
@Tag(name = "🏢 Entreprises", description = "Gestion des entreprises (multi-tenant)")
@SecurityRequirement(name = "bearerAuth")
public class EntrepriseController {

    private final EntrepriseService entrepriseService;

    @GetMapping
    @Operation(summary = "📋 Lister toutes les entreprises")
    public ResponseEntity<List<EntrepriseResponseDTO>> findAll() {
        return ResponseEntity.ok(entrepriseService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "🔍 Détail d'une entreprise")
    public ResponseEntity<EntrepriseResponseDTO> findById(
            @Parameter(description = "ID de l'entreprise") @PathVariable Long id
    ) {
        return ResponseEntity.ok(entrepriseService.findById(id));
    }

    @PostMapping
    @Operation(summary = "➕ Créer une entreprise")
    public ResponseEntity<EntrepriseResponseDTO> create(
            @Parameter(description = "Données de l'entreprise", required = true)
            @Valid @RequestBody EntrepriseRequestDTO dto
    ) {
        EntrepriseResponseDTO created = entrepriseService.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "✏️ Modifier une entreprise")
    public ResponseEntity<EntrepriseResponseDTO> update(
            @Parameter(description = "ID de l'entreprise") @PathVariable Long id,
            @Parameter(description = "Nouvelles données", required = true)
            @Valid @RequestBody EntrepriseRequestDTO dto
    ) {
        return ResponseEntity.ok(entrepriseService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "🗑️ Supprimer une entreprise")
    @ApiResponse(responseCode = "204", description = "✅ Entreprise supprimée")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de l'entreprise") @PathVariable Long id
    ) {
        entrepriseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
