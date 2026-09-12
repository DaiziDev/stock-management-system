# 📊 Analyse Comparative & Roadmap Priorisée — SGS

## Date : 28 août 2026

---

## 1. BENCHMARK : Les meilleurs systèmes de gestion de stock en 2026

### 1.1 Les systèmes analysés

| Système | Type | Cible | Forces principales |
|---------|------|-------|--------------------|
| **Odoo 19** | Open Source ERP | PME → Grande entreprise | ERP complet, 40+ modules, multi-entrepôt, barcode, lots/séries, routes avancées (drop-ship, cross-dock), comptabilité intégrée |
| **ERPNext 16** | Open Source ERP | PME → ETI | Stock ledger, réservation stock, réconciliation inventaire, numéros de série/lots, valuation FIFO/AVCO |
| **Zoho Inventory** | SaaS propriétaire | PME | Barcode/RFID, lots/séries, multi-entrepôt, conversion UoM, portal vendeur |
| **NetSuite** | SaaS ERP enterprise | Grande entreprise | AI forecasting, multi-entrepôt global, comptabilité native, dashboard temps réel |
| **inFlow** | SaaS propriétaire | PME | Interface intuitive, facturation intégrée, e-commerce Shopify/Magento |
| **Qoblex** | SaaS propriétaire | PME e-commerce | Synchronisation temps réel, forecasting IA, multi-canal |

### 1.2 Comparaison fonctionnelle détaillée

#### 🔴 = Non implémenté dans SGS | 🟡 = Partiellement implémenté | 🟢 = Implimenté

| Fonctionnalité | Odoo | ERPNext | Zoho | **SGS** |
|----------------|------|---------|------|---------|
| **CATALOGUE PRODUITS** | | | | |
| Gestion des catégories | 🟢 | 🟢 | 🟢 | 🟢 |
| Articles avec prix HT/TTC | 🟢 | 🟢 | 🟢 | 🟢 |
| Photos produits | 🟢 | 🟢 | 🟢 | 🔴 Champ `photo` existe mais pas utilisé |
| Variants produits (taille, couleur…) | 🟢 | 🟢 | 🟢 | 🔴 |
| Codes-barres par article | 🟢 | 🟢 | 🟢 | 🔴 |
| Codes séquentiels auto (ART-0001…) | 🟢 | 🟢 | 🟢 | 🔴 Code manuel uniquement |
| **STOCK** | | | | |
| Suivi stock en temps réel | 🟢 | 🟢 | 🟢 | 🔴 Entité `MvtStk` vide |
| Seuil de réapprovisionnement | 🟢 | 🟢 | 🟢 | 🟡 Champ `seuil` dans maquette, absent du backend |
| Alertes stock bas | 🟢 | 🟢 | 🟢 | 🔴 |
| Méthode valorisation (FIFO/AVCO) | 🟢 | 🟢 | 🟢 | 🔴 |
| Multi-entrepôts | 🟢 | 🟢 | 🟢 | 🔴 |
| Emplacements de stockage | 🟢 | 🟢 | 🟢 | 🔴 |
| Transferts inter-entrepôts | 🟢 | 🟢 | 🔴 | 🔴 |
| Lots / numéros de série | 🟢 | 🟢 | 🟢 | 🔴 |
| Dates d'expiration | 🟢 | 🟢 | 🟢 | 🔴 |
| Inventaire tournant (cycle counting) | 🟢 | 🟢 | 🟢 | 🔴 |
| **COMMANDES CLIENT** | | | | |
| Création de commande | 🟢 | 🟢 | 🟢 | 🟡 Entité partielle (code + date + client) |
| Lignes de commande | 🟢 | 🟢 | 🟢 | 🔴 `LigneCommandeClient` vide |
| Statuts de commande (en cours, validée…) | 🟢 | 🟢 | 🟢 | 🟡 Statut dans maquette, pas dans le backend |
| Validation/Annulation | 🟢 | 🟢 | 🟢 | 🔴 |
| Historique des commandes | 🟢 | 🟢 | 🟢 | 🔴 |
| **COMMANDES FOURNISSEUR** | | | | |
| Création de commande | 🟢 | 🟢 | 🟢 | 🔴 Coquille vide |
| Lignes de commande | 🟢 | 🟢 | 🟢 | 🔴 `LigneCommandeFournisseur` vide |
| Réception marchandise | 🟢 | 🟢 | 🟢 | 🔴 |
| Lien commande → mouvement stock | 🟢 | 🟢 | 🟢 | 🔴 |
| **VENTES / POS** | | | | |
| Point de vente | 🟢 | 🟢 | 🔴 | 🔴 |
| Lignes de vente | 🟢 | 🟢 | 🟢 | 🔴 `LigneVente` vide |
| Panier / mode rapide | 🟢 | 🟢 | 🔴 | 🔴 Maquette disponible |
| Facturation intégrée | 🟢 | 🟢 | 🔴 | 🔴 |
| **MOUVEMENTS DE STOCK** | | | | |
| Entrées (réceptions) | 🟢 | 🟢 | 🟢 | 🔴 Coquille vide |
| Sorties (ventes, commandes) | 🟢 | 🟢 | 🟢 | 🔴 |
| Ajustements manuels | 🟢 | 🟢 | 🟢 | 🔴 |
| Traçabilité complète | 🟢 | 🟢 | 🟢 | 🔴 |
| **TIERS** | | | | |
| Clients | 🟢 | 🟢 | 🟢 | 🟡 Entité partielle |
| Fournisseurs | 🟢 | 🟢 | 🟢 | 🔴 Coquille vide |
| Historique par tiers | 🟢 | 🟢 | 🟢 | 🔴 |
| **UTILISATEURS & RÔLES** | | | | |
| Gestion des utilisateurs | 🟢 | 🟢 | 🟢 | 🔴 Coquille vide |
| RBAC (contrôle d'accès par rôle) | 🟢 | 🟢 | 🟢 | 🟡 Frontend only, pas de backend |
| Audit trail / journal d'activité | 🟢 | 🟢 | 🟢 | 🔴 |
| **AUTHENTIFICATION** | | | | |
| Login / Logout | 🟢 | 🟢 | 🟢 | 🟡 Mock frontend uniquement |
| JWT / Session sécurisée | 🟢 | 🟢 | 🟢 | 🔴 Security commented out |
| Mot de passe hashé (BCrypt) | 🟢 | 🟢 | 🟢 | 🔴 |
| Réinitialisation mot de passe | 🟢 | 🟢 | 🟢 | 🔴 |
| **REPORTING** | | | | |
| Dashboard KPIs | 🟢 | 🟢 | 🟢 | 🟡 2 KPIs basiques |
| Rapport de stock | 🟢 | 🟢 | 🟢 | 🔴 Route `stockReport` dans nav, pas de page |
| Rapport de ventes | 🟢 | 🟢 | 🟢 | 🔴 |
| Rapport d'âge du stock | 🟢 | 🟢 | 🔴 | 🔴 |
| Export PDF / Excel | 🟢 | 🟢 | 🟢 | 🔴 |
| Graphiques (Chart.js) | 🟢 | 🟢 | 🟢 | 🔴 Chart.js dans maquette, pas dans Angular |
| **ENTREPRISE / MULTI-TENANT** | | | | |
| Gestion multi-entreprises | 🟢 | 🟢 | 🟢 | 🟡 Entité + API partielle |
| Isolation des données par entreprise | 🟢 | 🟢 | 🟢 | 🔴 Pas de filtre tenant côté backend |
| Switch d'entreprise | 🟢 | 🟢 | 🔴 | 🟡 Switcher dans navbar (cosmétique) |
| **DESIGN & UX** | | | | |
| Responsive (mobile) | 🟢 | 🟢 | 🟢 | 🟡 Design system fait, composants pas finis |
| Dark mode | 🟢 | 🔴 | 🔴 | 🟢 Automatique via prefers-color-scheme |
| Notifications | 🟢 | 🟢 | 🟢 | 🔴 Badge dans navbar, pas de système |
| Toasts / alertes | 🟢 | 🟢 | 🟢 | 🔴 Dans maquette, pas dans Angular |
| Modales | 🟢 | 🟢 | 🟢 | 🔴 Dans maquette, pas dans Angular |
| **AUTRES** | | | | |
| Email (notifications) | 🟢 | 🟢 | 🟢 | 🟡 SMTP configuré, pas utilisé |
| API documentation (Swagger) | 🟢 | 🟢 | 🔴 | 🟢 SpringDoc configuré |
| Import/Export CSV | 🟢 | 🟢 | 🟢 | 🔴 |
| Barcode scanning | 🟢 | 🟢 | 🟢 | 🔴 |
| Calcul automatique TTC | 🟢 | 🟢 | 🟢 | 🟢 Côté backend |
| Validation des formulaires | 🟢 | 🟢 | 🟢 | 🟢 Jakarta Validation + Reactive Forms |

### 1.3 Score de couverture SGS

| Catégorie | Fonctionnalités | Couvertes | % |
|-----------|----------------|-----------|---|
| Catalogue produits | 7 | 2 | 29% |
| Stock | 9 | 1 | 11% |
| Commandes client | 5 | 1 | 20% |
| Commandes fournisseur | 4 | 0 | 0% |
| Ventes / POS | 4 | 0 | 0% |
| Mouvements de stock | 4 | 0 | 0% |
| Tiers | 3 | 1 | 33% |
| Utilisateurs & rôles | 3 | 0 | 0% |
| Authentification | 4 | 1 | 25% |
| Reporting | 6 | 1 | 17% |
| Multi-tenant | 3 | 1 | 33% |
| Design & UX | 5 | 1 | 20% |
| Autres | 6 | 2 | 33% |
| **TOTAL** | **63** | **11** | **~17%** |

---

## 2. CE QUI RESTE À FAIRE — Roadmap priorisée

### Légende des priorités
- **P0 — Critique** : Sans ça, l'app ne fonctionne pas du tout
- **P1 — Essentiel** : Fonctionnalités attendues par tout utilisateur
- **P2 — Important** : Met l'app au niveau des concurrents
- **P3 — Nice-to-have** : Évolutifs, à faire plus tard

---

### 🔴 P0 — CRITIQUE (Semaine 1-2)
> *Sans ces éléments, l'application ne peut pas être utilisée en production.*

#### 1. Authentification Backend (Spring Security + JWT)
**Pourquoi en premier ?** Sans sécurité, toute l'API est exposée. C'est non-négociable.

**Comment s'y prendre :**
```
Étape 1 — Activer les dépendances dans pom.xml
  → Décommenter spring-boot-starter-security
  → Décommenter jjwt-api, jjwt-impl, jjwt-jackson (v0.12.6)

Étape 2 — Créer l'entité Utilisateur avec tous les champs
  → nom, prenom, login (unique), motDePasse (hashé BCrypt),
     mail, numTel, role (enum: ADMIN/VENDEUR/GESTIONNAIRE),
     entreprise (ManyToOne → Entreprise)

Étape 3 — Créer l'entité Roles
  → id, nom (ADMIN, VENDEUR, GESTIONNAIRE)

Étape 4 — Créer AuthController
  → POST /api/auth/login → retourne JWT
  → POST /api/auth/register (ADMIN uniquement)
  → POST /api/auth/logout
  → GET  /api/auth/me → utilisateur courant

Étape 5 — Créer JwtUtil (utilitaire JWT)
  → generateToken(email, role, entrepriseId)
  → validateToken(token)
  → extractEmail(token)
  → Secret key dans application.yaml

Étape 6 — Créer JwtAuthFilter (OncePerRequestFilter)
  → Lit le header Authorization: Bearer <token>
  → Valide le token
  → Charge l'utilisateur dans le SecurityContext

Étape 7 — Configurer SecurityFilterChain
  → /api/auth/** → permitAll
  → /api/** → authenticated
  → CORS configuré pour Angular (localhost:4200)
  → Stateless session

Étape 8 — Créer UtilisateurService + DTOs
  → UtilisateurRequestDTO, UtilisateurResponseDTO
  → LoginRequestDTO { email, password }
  → LoginResponseDTO { token, user }
```

#### 2. Entité Utilisateur complète + CRUD
**Pourquoi ?** Pas de gestion de comptes possible sans ça.

**Champs à ajouter :**
```java
@Entity
public class Utilisateur extends AbstractEntity {
    @Column(nullable = false) private String nom;
    @Column(nullable = false) private String prenom;
    @Column(nullable = false, unique = true) private String login;
    @Column(nullable = false) private String motDePasse; // BCrypt
    private String mail;
    private String numTel;
    private String photo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private UserRole role; // ADMIN, VENDEUR, GESTIONNAIRE

    @ManyToOne
    @JoinColumn(name = "identreprise")
    private Entreprise entreprise;
}
```

#### 3. Filtrage Multi-Tenant côté Backend
**Pourquoi ?** Sans isolation, une entreprise voit les données d'une autre. C'est une faille de sécurité critique.

**Comment s'y prendre :**
```
Option A (recommandée) — Filtre JPA automatique :
  1. Créer TenantContext.java (ThreadLocal pour stocker l'entrepriseId du token JWT)
  2. Créer TenantFilter.java (Hibernate filter ajouté à chaque requête)
  3. Dans JwtAuthFilter → extraire entrepriseId du JWT → mettre dans TenantContext
  4. Créer une annotation @TenantFiltered + un aspect Hibernate

Option B (plus simple) — Filtre dans chaque Service :
  1. Extraire entrepriseId du SecurityContext dans chaque Service
  2. Ajouter .where(entreprise.id = :entrepriseId) dans chaque Repository
  3. Plus manuel mais plus transparent
```

#### 4. Entité Roles complète
```java
@Entity
public class Roles extends AbstractEntity {
    @Column(nullable = false, unique = true) private String nom;
    // ADMIN, VENDEUR, GESTIONNAIRE
    // Plus tard : permissions granulaires
}
```

---

### 🟠 P1 — ESSENTIEL (Semaine 3-5)
> *Les fonctionnalités de base attendues par tout utilisateur d'un logiciel de stock.*

#### 5. Client — Entité complète + CRUD Backend + Frontend
**Comment s'y prendre :**
```
Backend :
  1. Compléter l'entité Client avec TOUS les champs :
     nom, prenom, mail, numTel, photo
     adresse (embedded: adresse1, adresse2, ville, codePostal, pays)
     entreprise (ManyToOne)
     commandeClients (OneToMany — pas de mappedBy tant que LigneCommandeClient n'existe pas)

  2. Créer ClientController : CRUD /api/clients
  3. Créer ClientService : logique métier
  4. Créer ClientRepository : findByEntrepriseId
  5. Créer ClientRequestDTO / ClientResponseDTO
  6. Ajouter filtre multi-tenant

Frontend :
  1. Créer client.model.ts (interface Client, ClientRequest)
  2. Créer ClientService (HttpClient, signal)
  3. Créer ClientList component (table + recherche + suppression)
  4. Créer ClientForm component (formulaire reactive)
  5. Créer client.routes.ts
  6. Ajouter la route dans app.routes.ts
```

#### 6. Fournisseur — Entité complète + CRUD Backend + Frontend
**Identique au Client mais sans commandeClients.**
```
Backend :
  1. Compléter Fournisseur : nom, mail, numTel, adresse (embedded), entreprise
  2. Créer FournisseurController, Service, Repository, DTOs
  3. CRUD /api/fournisseurs

Frontend :
  1. Même pattern que Client (model, service, list, form, routes)
```

#### 7. LigneCommandeClient — Entité + Relations
**Pourquoi ?** Sans les lignes, une commande client n'a pas de contenu.

```
Backend :
  1. Créer LigneCommandeClient avec :
     qte (Integer, NOT NULL)
     prixUnitaire (BigDecimal)
     commandeClient (ManyToOne → CommandeClient) ← CRUCIAL : c'est ce champ
       qui permet de réactiver le mappedBy côté CommandeClient
     article (ManyToOne → Article)

  2. Réactiver dans CommandeClient :
     @OneToMany(mappedBy = "commandeClient")
     private List<LigneCommandeClient> ligneCommandeClients;

  3. Créer LigneCommandeClientController (sous /api/commandes-client/{id}/lignes)
  4. Créer Service + DTOs
```

#### 8. CommandeClient — Compléter + CRUD
```
Backend :
  1. Ajouter : statut (enum: BROUILLON, EN_COURS, VALIDEE, ANNULEE, LIVREE)
     Calcul automatique du statut selon les validations

  2. Créer CommandeClientController : CRUD /api/commandes-client
     POST /api/commandes-client → crée avec lignes
     PUT /api/commandes-client/{id}/valider → passe à VALIDEE
     PUT /api/commandes-client/{id}/annuler → passe à ANNULEE

  3. Lors de la validation → générer automatiquement un MouvementStk (SORTIE)

Frontend :
  1. Créer CommandeClientService
  2. Créer CommandeClientList (table avec statuts, couleurs)
  3. Créer CommandeClientForm (avec ajout/suppression dynamique de lignes)
  4. Créer route + ajouter dans app.routes.ts
```

#### 9. MouvementStock — Entité + CRUD
```
Backend :
  1. Créer MvtStk avec :
     type (enum: ENTREE, SORTIE, AJUSTEMENT, TRANSFERT)
     qte (Integer)
     dateMouvement (LocalDateTime)
     motif (String)
     article (ManyToOne → Article)
     commandeClient (ManyToOne, nullable)
     commandeFournisseur (ManyToOne, nullable)
     entreprise (ManyToOne)
     crePar (ManyToOne → Utilisateur) — qui a fait le mouvement

  2. Créer MvtStkController : /api/mouvements-stock
     GET → liste avec filtres (date, article, type)
     POST → création manuelle (ajustement)

  3. Créer MvtStkService avec logique :
     entrée → +stock sur l'article
     sortie → -vérifier stock suffisant → -stock
     ajustement → correction manuelle

  ⚠️ IMPORTANT : recalculer le stock de l'article à chaque mouvement :
     stock_actuel = stock_initial + sum(ENTREE) - sum(SORTIE)
```

#### 10. LigneCommandeFournisseur + CommandeFournisseur
**Même pattern que les commandes client mais dans l'autre sens (ENTREE de stock).**
```
Backend :
  1. Créer LigneCommandeFournisseur (qte, article, commandeFournisseur)
  2. Compléter CommandeFournisseur (fournisseur, dateCommande, statut)
  3. Lors de validation → générer MouvementStk de type ENTREE
  4. Mettre à jour le stock de l'article

Frontend :
  1. Même pattern CRUD
```

#### 11. LigneVente + Vente
```
Backend :
  1. Créer LigneVente (qte, prixUnitaire, article, vente)
  2. Compléter Vente (date, client (nullable pour ventes comptoir), lignes)
  3. Lors de création → générer MouvementStk de type SORTIE

Frontend :
  1. Service + List + Form
```

---

### 🟡 P2 — IMPORTANT (Semaine 6-8)
> *Ce qui distingue un app basique d'un vrai outil de gestion.*

#### 12. Gestion des stocks — Logique métier
```
Backend :
  1. Ajouter à l'entité Article :
     stockActuel (Integer) — calculé ou maintenu en temps réel
     stockMinimum (Integer) — seuil d'alerte
     stockSecurite (Integer) — tampon
     pointCommande (Integer) — calculé

  2. Créer StockService :
     getStockDisponible(articleId) → stockActuel - réservations
     verifierStock(articleId, qteDemandee) → boolean
     alertesStockBas() → Liste<Article> où stock < stockMinimum

  3. Créer StockController :
     GET /api/stock/alertes → articles en alerte
     GET /api/stock/valorisation → valeur totale du stock
     GET /api/stock/rotation → taux de rotation
```

#### 13. Tableau de bord enrichi (Dashboard)
```
Frontend :
  1. KPIs à afficher :
     - Valeur totale du stock (prix TTC × stock)
     - Nombre de produits en alerte stock bas
     - Ventes du mois
     - Commandes en cours
     - Chiffre d'affaires

  2. Graphiques (Chart.js est déjà dans la maquette) :
     - Ventes par mois (bar chart)
     - Répartition du stock par catégorie (pie chart)
     - Top 5 articles les plus vendus

  3. Activité récente :
     - Derniers mouvements de stock
     - Dernières commandes

  4. Créer DashboardService avec appels API
```

#### 14. Point de vente (POS)
```
Frontend :
  1. Créer la vue POS (grille de produits + panier)
     → Reprendre la maquette maquette.html (section POS)
     → Grille de produits cliquables
     → Panier latéral avec quantités
     → Total en temps réel
     → Bouton "Valider la vente"

  2. Créer PosService :
     - chargerArticles() → articles disponibles
     - ajouterAuPanier(article)
     - retirerDuPanier(articleId)
     - validerVente(client?, lignes)

  3. Backend : endpoint POST /api/ventes
     → Crée la vente + lignes + mouvement de stock
```

#### 15. Alertes & Notifications
```
Backend :
  1. Créer notification automatique quand stock < seuil
  2. Utiliser Spring Mail (déjà configuré) pour envoyer un email
  3. Créer endpoint GET /api/notifications

Frontend :
  1. Système de notifications (badge navbar)
  2. Toasts pour les actions ( créer, modifier, supprimer)
     → La maquette a déjà le design des toasts
```

---

### 🔵 P3 — NICE-TO-HAVE (Semaine 9-12)
> *Évolutifs pour rivaliser avec les leaders du marché.*

#### 16. Rapports & Analytics
```
Frontend :
  1. Page Rapports & Stock (route /rapports déjà prévue dans NAV)
  2. Rapport de stock : tableau avec tous les articles, stock actuel, valeur
  3. Rapport de ventes : ventes par période, par client, par article
  4. Rapport de mouvements : historique filtrable
  5. Export PDF (utiliser une librairie comme jsPDF ou une endpoint backend)

Backend :
  1. Créer des endpoints de reporting avec agrégations SQL
  2. GET /api/reports/stock?date=…
  3. GET /api/reports/ventes?from=…&to=…
  4. GET /api/reports/mouvements?articleId=…
```

#### 17. Gestion multi-entrepôt
```
Backend :
  1. Créer entité Entrepot (nom, adresse, entreprise)
  2. Ajouter entrepôt aux mouvements de stock
  3. Transferts inter-entrepôts

Frontend :
  1. Sélecteur d'entrepôt dans le POS
  2. Vue multi-entrepôt dans les rapports
```

#### 18. Codes-barres
```
Frontend :
  1. Génération de codes-barres pour chaque article (librairie JsBarcode)
  2. Affichage sur les fiches article

Backend :
  1. Champ codeBarre dans Article
  2. Endpoint de recherche par code-barre
```

#### 19. Import/Export CSV
```
Backend :
  1. Endpoint POST /api/articles/import → reçoit un CSV
  2. Endpoint GET /api/articles/export → retourne un CSV
  3. Utiliser OpenCSV ou Apache Commons CSV

Frontend :
  1. Bouton "Importer" sur les listes
  2. Bouton "Exporter" sur les listes
```

#### 20. Audit Trail
```
Backend :
  1. Créer entite AuditLog (utilisateur, action, entite, date, details)
  2. Aspect AOP qui log toute action CRUD
  3. Endpoint GET /api/audit-logs
```

#### 21. Rôles granulaires & permissions
```
Backend :
  1. Table role_permissions (roleId, permission)
  2. Permissions : ARTICLE_READ, ARTICLE_WRITE, STOCK_READ, STOCK_ADJUST,
     COMMANDE_VALIDATE, VENTE_CREATE, USER_MANAGE, etc.
  3. Vérifier les permissions dans chaque controller via @PreAuthorize
```

#### 22. Mot de passe oublié
```
Backend :
  1. Endpoint POST /api/auth/forgot-password → envoie email avec token
  2. Endpoint POST /api/auth/reset-password → vérifie token, change MDP
  3. Utiliser Spring Mail (déjà configuré)

Frontend :
  1. Page "Mot de passe oublié"
  2. Page "Réinitialisation"
```

#### 23. Responsive & Polish UI
```
Frontend :
  1. Appliquer le design system Tailwind (déjà dans styles.css) à TOUS les composants
  2. Les listes (articles, catégories, clients…) sont encore en HTML brut sans style
  3. Reprendre le style de la maquette maquette.html :
     - Tables avec badges de statut
     - Modales pour création/modification
     - Toasts pour les confirmations
     - KPI cards sur le dashboard
     - Responsive mobile (sidebar repliable)
```

---

## 3. PLAN D'EXÉCUTION RECOMMANDÉ

### Sprint 1 (Semaines 1-2) — Sécurité & Fondations
```
Jour 1-2  : Spring Security + JWT + AuthController
Jour 3    : Entité Utilisateur complète + DTOs
Jour 4    : Entité Roles + CRUD
Jour 5-6  : Filtrage multi-tenant (TenantContext + filtres)
Jour 7    : Tests d'intégration auth (login, token, accès refusé)
Jour 8    : Corriger les entités coquilles (ajouter les champs manquants)
```

### Sprint 2 (Semaines 3-4) — Tiers & Commandes
```
Jour 1-2  : Client → Backend complet (entité, controller, service, DTOs, filtre tenant)
Jour 3    : Client → Frontend (model, service, list, form, routes)
Jour 4-5  : Fournisseur → Même pattern
Jour 6-7  : LigneCommandeClient + réactiver relation CommandeClient
Jour 8    : CommandeClient → Backend complet avec statuts
Jour 9    : CommandeClient → Frontend
Jour 10   : Tests
```

### Sprint 3 (Semaines 5-6) — Stock & Ventes
```
Jour 1-2  : MvtStk → Backend complet avec logique de calcul de stock
Jour 3    : LigneCommandeFournisseur + CommandeFournisseur
Jour 4    : Validation commande → mouvement de stock automatique
Jour 5-6  : LigneVente + Vente → Backend
Jour 7-8  : Point de vente → Frontend (grille + panier)
Jour 9-10 : Tests d'intégration bout en bout
```

### Sprint 4 (Semaines 7-8) — Dashboard & Polish
```
Jour 1-3  : Dashboard enrichi (KPIs, graphiques, alertes)
Jour 4-5  : Alertes de stock bas + notifications
Jour 6-7  : Rapports & Stock (page /rapports)
Jour 8-10 : Polish UI — appliquer le design system Tailwind partout
```

### Sprint 5 (Semaines 9-10) — Features avancées
```
Jour 1-2  : Import/Export CSV
Jour 3-4  : Codes-barres
Jour 5-6  : Audit trail
Jour 7-8  : Rôles granulaires
Jour 9-10 : Mot de passe oublié
```

---

## 4. RÉSUMÉ EXÉCUTIF

| Métrique | Valeur |
|----------|--------|
| **Fonctionnalités totales identifiées** | 63 |
| **Déjà implémentées** | 11 (~17%) |
| **À implémenter** | 52 |
| **Sprints estimés** | 5 (10 semaines) |
| **Entités backend à compléter** | 9 (sur 14) |
| **Features frontend à créer** | 8 (sur 11) |
| **Écart vs Odoo/ERPNext** | Modeste : même architecture, mais 83% du périmètre reste à construire |

### Ce qui est bien fait et à conserver :
- Architecture backend propre (Controller/Service/Repository)
- Design system Tailwind complet et bien documenté
- DTOs records Java (bon pattern)
- Gestion d'erreurs globale (GlobalExceptionHandler)
- Maquette HTML exhaustive (référence UI complète)
- RBAC frontend opérationnel
- Multi-tenant dès le départ (même si pas encore actif côté backend)

### Les risques principaux :
1. **Multi-tenant non isolé** → faille de sécurité critique
2. **Pas d'auth backend** → API exposée sans restriction
3. **Stock non calculé** → les mouvements n'ont pas d'impact sur les articles
4. **Relations JPA cassées** → les lignes de commande sont vides, les relations OneToMany sont commentées

---

*Document généré le 28 août 2026 — Basé sur l'analyse de Odoo 19, ERPNext 16, Zoho Inventory, NetSuite, inFlow, Qoblex et les meilleures pratiques du secteur.*
