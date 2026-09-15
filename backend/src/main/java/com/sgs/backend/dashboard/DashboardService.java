package com.sgs.backend.dashboard;

import com.sgs.backend.commande.StatutCommandeClient;
import com.sgs.backend.commande.StatutCommandeFournisseur;
import com.sgs.backend.commandeClient.CommandeClientRepository;
import com.sgs.backend.commandeFournisseur.CommandeFournisseurRepository;
import com.sgs.backend.config.CurrentUserService;
import com.sgs.backend.dashboard.dto.DashboardKpisDTO;
import com.sgs.backend.stock.StockService;
import com.sgs.backend.vente.dto.VenteResponseDTO;
import com.sgs.backend.vente.VenteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Agrège des données déjà tenant-scopées par les Services sous-jacents --
 * ce Service ne fait aucun filtrage lui-même, il combine des résultats déjà
 * sûrs (chacun applique son propre CurrentUserService en interne).
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final StockService stockService;
    private final CommandeClientRepository commandeClientRepository;
    private final CommandeFournisseurRepository commandeFournisseurRepository;
    private final VenteService venteService;
    private final CurrentUserService currentUserService;

    public DashboardKpisDTO kpis() {
        Long entrepriseId = currentUserService.getEntrepriseId();

        BigDecimal valeurStock = stockService.valorisation().valeurTotale();
        int nbArticlesEnAlerte = stockService.alertes().size();
        long nbCommandesClientEnCours = commandeClientRepository.countByEntrepriseIdAndStatut(entrepriseId, StatutCommandeClient.EN_COURS);
        long nbCommandesFournisseurEnAttente = commandeFournisseurRepository.countByEntrepriseIdAndStatut(entrepriseId, StatutCommandeFournisseur.EN_ATTENTE);

        Instant debutDuMois = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<VenteResponseDTO> ventesDuMois = venteService.findAll().stream()
                .filter(v -> !v.dateVente().isBefore(debutDuMois))
                .toList();
        BigDecimal chiffreAffairesDuMois = ventesDuMois.stream()
                .map(VenteResponseDTO::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DashboardKpisDTO(
                valeurStock,
                nbArticlesEnAlerte,
                nbCommandesClientEnCours,
                nbCommandesFournisseurEnAttente,
                ventesDuMois.size(),
                chiffreAffairesDuMois
        );
    }
}
