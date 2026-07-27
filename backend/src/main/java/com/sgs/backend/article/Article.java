package com.sgs.backend.article;

import com.sgs.backend.categorie.Categorie;
import com.sgs.backend.common.AbstractEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "article")
public class Article extends AbstractEntity {

    @Column(name = "codearticle", nullable = false, unique = true)
    private String codeArticle;

    @Column(name = "designation", nullable = false)
    private String designation;

    @Column(name = "prixunitaireht", nullable = false)
    private BigDecimal prixUnitaireHt;

    @Column(name = "tauxtva", nullable = false)
    private BigDecimal tauxTva;

    @Column(name = "prixunitairettc", nullable = false)
    private BigDecimal prixUnitaireTtc;

    @Column(name = "photo")
    private String photo;

    @ManyToOne
    @JoinColumn(name = "idcategorie")
    private Categorie categorie;
}
