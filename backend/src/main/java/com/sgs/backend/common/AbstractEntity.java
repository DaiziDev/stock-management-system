package com.sgs.backend.common;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Getter                                    // Lombok génère automatiquement tous les getters
@Setter                                    // Lombok génère automatiquement tous les setters
@MappedSuperclass                          // Dit à JPA que cette classe n'est pas une table
// mais ses champs seront copiés dans les tables enfants
@EntityListeners(AuditingEntityListener.class) // Active la gestion automatique des dates
public abstract class AbstractEntity {     // abstract = on ne peut pas créer un AbstractEntity
    // directement, seulement ses enfants

    @Id                                    // Ce champ est la clé primaire
    @GeneratedValue(strategy = GenerationType.IDENTITY) // PostgreSQL génère l'id automatiquement
    private Long id;                       // Long au lieu de Integer = supporte plus de valeurs

    @CreatedDate                           // Spring remplit ce champ automatiquement à la création
    @Column(updatable = false,             // updatable=false = ce champ ne changera jamais après création
            nullable = false)              // nullable=false = ce champ est obligatoire en base
    private LocalDateTime createdAt;       // LocalDateTime = date + heure, moderne et fiable

    @LastModifiedDate                      // Spring remplit ce champ automatiquement à chaque modification
    @Column(nullable = false)              // obligatoire en base
    private LocalDateTime updatedAt;
}