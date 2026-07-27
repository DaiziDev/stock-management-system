package com.sgs.backend.commandeClient;

import com.sgs.backend.client.Client;
import com.sgs.backend.common.AbstractEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "commandeclient")
public class CommandeClient extends AbstractEntity {

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "datecommande", nullable = false)
    private Instant dateCommande;

    @ManyToOne
    @JoinColumn(name = "idclient")
    private Client client;

    // TODO (roadmap #7) : réactiver cette relation une fois LigneCommandeClient
    // construite avec son champ `commandeClient` (@ManyToOne côté LigneCommandeClient).
    // Hibernate valide TOUTES les entités au démarrage : une relation mappedBy
    // pointant vers un champ inexistant empêche TOUTE l'application de démarrer,
    // pas seulement CommandeClient. D'où la désactivation temporaire.
    // @OneToMany(mappedBy = "commandeClient")
    // private List<LigneCommandeClient> ligneCommandeClients;
}
