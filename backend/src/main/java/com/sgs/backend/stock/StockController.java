package com.sgs.backend.stock;

import com.sgs.backend.stock.dto.ArticleStockDTO;
import com.sgs.backend.stock.dto.ValorisationResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.sgs.backend.config.SecurityRoles;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Tag(name = "📊 Stock", description = "Consultation des niveaux de stock (lecture seule)")
@SecurityRequirement(name = "bearerAuth")
public class StockController {

    private final StockService stockService;

    @GetMapping("/etat")
    @PreAuthorize(SecurityRoles.TOUS)
    @Operation(summary = "📋 État du stock", description = "Stock actuel et seuil de chaque article.")
    public ResponseEntity<List<ArticleStockDTO>> etat() {
        return ResponseEntity.ok(stockService.etat());
    }

    @GetMapping("/alertes")
    @PreAuthorize(SecurityRoles.TOUS)
    @Operation(summary = "🔔 Articles en alerte", description = "Articles dont le stock est descendu au niveau du seuil minimum ou en dessous.")
    public ResponseEntity<List<ArticleStockDTO>> alertes() {
        return ResponseEntity.ok(stockService.alertes());
    }

    @GetMapping("/valorisation")
    @PreAuthorize(SecurityRoles.GESTION)
    @Operation(summary = "💰 Valorisation du stock", description = "Valeur totale du stock (prix HT × quantité, tous articles confondus).")
    public ResponseEntity<ValorisationResponseDTO> valorisation() {
        return ResponseEntity.ok(stockService.valorisation());
    }
}
