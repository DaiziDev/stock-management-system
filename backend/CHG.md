# Cahier des Charges Fonctionnel — SGS (Système de Gestion de Stock)

> Document généré à partir de l'analyse du code source réellement implémenté (`backend`).
> Version : 1.0 — Septembre 2026

---

## 1. Présentation du projet

### 1.1 Objectif

SGS est une **application de gestion de stock multi-entreprise** fonctionnant comme
une API REST. Elle permet à une entreprise de gérer son catalogue d'articles, ses
stocks, ses clients, ses fournisseurs, ses commandes (achats et ventes) et ses
utilisateurs, avec une traçabilité complète de chaque mouvement de stock.

### 1.2 Périmètre couvert (implémenté)

| Domaine | État |
|---|---|
| Authentification & sécurité JWT | ✅ Implémenté |
| Gestion multi-entreprises (multi-tenant) | ✅ Implémenté |
| Gestion des utilisateurs & rôles | ✅ Implémenté |
| Catégories & Articles | ✅ Implémenté |
| Clients & Fournisseurs (fiches) | ✅ Implémenté |
| Commandes clients (avec workflow de validation) | ✅ Implémenté |
| Commandes fournisseurs (avec réception) | ✅ Implémenté |
| Ventes au comptoir | ✅ Implémenté |
| Mouvements de stock & ajustements | ✅ Implémenté |
| État du stock, alertes, valorisation | ✅ Implémenté |
| Notifications in-app (stock bas) | ✅ Implémenté |
| Emails automatiques (commandes) | ✅ Implémenté (best-effort) |
| Tableau de bord (KPIs) | ✅ Implémenté |
| Documentation API interactive (Swagger) | ✅ Implémenté |

### 1.3 Stack technique

- **Java 17** — Spring Boot **3.4.1** (Web, Data JPA, Validation, Security, Mail)
- **PostgreSQL** — base de données relationnelle
- **Spring Security + JWT** (jjwt 0.12.6) — authentification stateless
- **Lombok** — réduction du boilerplate
- **springdoc-openapi 2.8.4** — documentation Swagger UI
- **Maven** (wrapper inclus) — build

---

## 2. Acteurs et rôles

Trois rôles existent (énumération `UserRole`) :

| Rôle | Description | Accès dans l'implémentation actuelle |
|---|---|---|
| **ADMIN** | Accès total : gère les entreprises, les comptes utilisateurs, les paramètres | Création de comptes (`/register`), gestion des utilisateurs de son entreprise, CRUD entreprises |
| **GESTIONNAIRE** | Gère les commandes, fournisseurs, stock et rapports | Accès métier (articles, commandes, stock, ventes…) |
| **VENDEUR** | Vente au comptoir + consultation articles/clients | Accès métier (ventes, consultation) |

> **Note d'implémentation** : la différenciation des droits par endpoint est
> appliquée côté serveur par `@PreAuthorize` sur chacun des 52 endpoints. Les
> expressions sont centralisées dans `SecurityRoles` (package `config`), qui
> définit trois niveaux : `ADMIN` (entreprises, comptes), `GESTION` =
> ADMIN+GESTIONNAIRE (catalogue en écriture, commandes, fournisseurs,
> mouvements de stock, valorisation, KPIs) et `TOUS` = les trois rôles
> (consultation catalogue/clients/stock, ventes, notifications).
>
> Le rôle qui fait autorité est celui lu **en base** à chaque requête
> (`JwtAuthFilter` utilise les autorités de `UserDetails`), et non le claim
> `role` du token : sans mécanisme de révocation, un token de 24 h laisserait
> sinon un utilisateur rétrogradé conserver ses droits jusqu'à expiration.
> Le claim reste présent pour l'affichage côté frontend.

### 2.1 Multi-tenant (multi-entreprises)

- Chaque donnée métier (article, client, commande, vente, mouvement…) porte une
  référence vers son **entreprise** (`identreprise`).
- L'entreprise de l'utilisateur connecté est déduite de son **JWT** au moment de
  la requête (composant `CurrentUserService`).
- **Règle d'isolation** : toute ressource appartenant à une autre entreprise est
  traitée comme *introuvable* (404), jamais comme *interdite* (403) — afin de ne
  pas révéler l'existence de données d'autres entreprises.

---

## 3. Sécurité et authentification

### 3.1 Principe

- API **stateless** : aucune session serveur ; chaque requête doit porter le JWT
  dans l'en-tête `Authorization: Bearer <token>`.
- Mots de passe **jamais stockés en clair** : hashage **BCrypt** côté backend.
- CSRF désactivé (inutile pour une API stateless sans cookies de session).

### 3.2 Endpoints d'authentification

| Endpoint | Méthode | Accès | Description |
|---|---|---|---|
| `/api/auth/login` | POST | Public | Vérifie login + mot de passe, retourne un JWT + les infos utilisateur |
| `/api/auth/register` | POST | ADMIN | Crée un compte utilisateur dans une entreprise (mot de passe hashé) |
| `/api/auth/me` | GET | Authentifié | Retourne le profil de l'utilisateur connecté (déduit du token) |

### 3.3 Contenu du JWT

Le token contient : le **login**, le **rôle** et l'**identifiant d'entreprise**
de l'utilisateur — ce qui permet le filtrage multi-tenant à chaque requête.

### 3.4 Compte de bootstrap (amorçage)

Au premier démarrage (base vide), un initialiseur (`DataInitializer`) crée :

- L'entreprise **« SGS Demo »** — uniquement si aucune entreprise n'existe encore
- Un administrateur : login **`ADMIN_LOGIN`** (défaut `admin@sgs.local`), mot de
  passe **`ADMIN_PASSWORD`** — **variable d'environnement requise**

> ⚠️ Aucun identifiant par défaut n'est committé dans le code : sans
> `ADMIN_PASSWORD`, l'application refuse de démarrer. Copier `.env.example`
> vers `.env` et renseigner les secrets (`JWT_SECRET`, `DB_PASSWORD`,
> `ADMIN_PASSWORD`…). Voir §7 pour la liste complète.

### 3.5 Rate limiting du login (anti brute force)

`POST /api/auth/login` — seul endpoint public — est protégé par
`LoginRateLimitFilter` : après **5 échecs** de connexion depuis une même IP
dans une fenêtre de **5 minutes**, toute nouvelle tentative reçoit un **429**
(avec en-tête `Retry-After`) sans même atteindre la vérification du mot de
passe. Une connexion **réussie** remet le compteur à zéro ; la fenêtre
expirée, les compteurs repartent de zéro (blocage temporaire).

Seuls les échecs d'authentification (401/403) sont comptés : un utilisateur
légitime derrière une IP partagée n'est pas pénalisé tant qu'il finit par se
connecter. Compteurs en mémoire (instance mono-nœud) ; à déplacer vers un
store partagé (Redis…) en cas de déploiement multi-instances. Réglage :
`app.rate-limit.login.max-failures` / `window-seconds`.

---

## 4. Modules fonctionnels

### 4.1 Gestion des entreprises

| Endpoint | Description |
|---|---|
| `GET /api/entreprises` | Lister les entreprises |
| `GET /api/entreprises/{id}` | Détail d'une entreprise |
| `POST /api/entreprises` | Créer une entreprise |
| `PUT /api/entreprises/{id}` | Modifier une entreprise |
| `DELETE /api/entreprises/{id}` | Supprimer une entreprise |

Une entreprise possède : un **nom unique**, une **adresse** (rue, ville, code
postal, pays), un **email** et un **numéro de téléphone**.

---

### 4.2 Gestion des utilisateurs

Réservé au rôle **ADMIN**, limité aux utilisateurs de **sa propre entreprise**.

| Endpoint | Description |
|---|---|
| `GET /api/utilisateurs` | Lister les utilisateurs de mon entreprise |
| `GET /api/utilisateurs/{id}` | Détail d'un utilisateur |
| `PUT /api/utilisateurs/{id}` | Modifier nom, prénom, contact et rôle |
| `DELETE /api/utilisateurs/{id}` | Supprimer un utilisateur |

Règles :
- Le **login est unique** ; sa modification n'est pas prévue (pas plus que le
  mot de passe, qui relève d'endpoints dédiés à venir).
- La création passe exclusivement par `POST /api/auth/register` (réservé ADMIN).
- Un utilisateur ne peut pas voir/gérer les comptes d'une autre entreprise.

---

### 4.3 Catégories

CRUD complet sur `/api/categories` (lister, détail, créer, modifier, supprimer).

Une catégorie possède : un **code unique** et une **désignation**. Elle peut
regrouper plusieurs articles.

---

### 4.4 Articles

CRUD complet sur `/api/articles`.

Champs d'un article :

| Champ | Règle |
|---|---|
| `codeArticle` | **Unique**, obligatoire |
| `designation` | Obligatoire |
| `prixUnitaireHt` | Prix d'achat / HT, obligatoire |
| `tauxTva` | Taux de TVA, obligatoire |
| `prixUnitaireTtc` | Prix TTC (base des ventes), obligatoire |
| `photo` | Référence/URL d'image (optionnel) |
| `categorie` | Rattachement optionnel à une catégorie |
| `stockActuel` | **Jamais modifiable directement** : seule la passe par un mouvement de stock le change (voir §4.9) |
| `seuilMin` | Seuil d'alerte optionnel — non renseigné = pas d'alerte pour cet article |

---

### 4.5 Clients

CRUD complet sur `/api/clients`.

Fiche client : **nom**, **prénom** (obligatoires), adresse, photo, email,
téléphone. Le client est rattaché à l'entreprise de l'utilisateur créateur et
sert aux **commandes clients** et aux **ventes**.

---

### 4.6 Fournisseurs

CRUD complet sur `/api/fournisseurs`.

Fiche fournisseur : **nom** (obligatoire), adresse, email, téléphone. Rattaché
à l'entreprise. Sert aux **commandes fournisseurs**.

---

### 4.7 Commandes clients

Endpoints sur `/api/commandes-client` :

| Endpoint | Description |
|---|---|
| `GET /api/commandes-client` | Lister les commandes de mon entreprise |
| `GET /api/commandes-client/{id}` | Détail d'une commande |
| `POST /api/commandes-client` | Créer une commande (statut initial `EN_COURS`) |
| `PUT /api/commandes-client/{id}/valider` | Valider → déclenche les **sorties de stock** |
| `PUT /api/commandes-client/{id}/annuler` | Annuler (si encore `EN_COURS`) |

**Cycle de vie :**

```
EN_COURS ──valider──> VALIDEE   (irréversible, sorties de stock générées)
EN_COURS ──annuler──> ANNULEE   (aucun impact stock, rien n'a bougé)
```

**Règles :**
- À la création, le **prix TTC de chaque ligne est figé** (snapshot) : une
  évolution ultérieure du prix de l'article ne modifie pas la commande.
- La création de la commande et de ses lignes est **transactionnelle** : si un
  article n'existe pas, rien n'est enregistré.
- Chaque commande reçoit un **code lisible** `CC-000001`, `CC-000002`…
- **Validation** : seule une commande `EN_COURS` peut être validée ; la
  validation génère une **sortie de stock par ligne** (RG-02). Si **une seule
  ligne** manque de stock, **toute l'opération est annulée** (rollback) — pas
  de validation partielle.
- **Annulation** : seule une commande `EN_COURS` peut être annulée. Une commande
  `VALIDEE` ne peut pas l'être (il faudrait des mouvements inverses — hors
  périmètre actuel).
- Un **email de confirmation** est envoyé au client à la création (RG-08,
  best-effort : un échec d'envoi ne bloque jamais la commande).

---

### 4.8 Commandes fournisseurs

Endpoints sur `/api/commandes-fournisseur` :

| Endpoint | Description |
|---|---|
| `GET /api/commandes-fournisseur` | Lister les commandes de mon entreprise |
| `GET /api/commandes-fournisseur/{id}` | Détail d'une commande |
| `POST /api/commandes-fournisseur` | Créer (statut initial `EN_ATTENTE`) |
| `PUT /api/commandes-fournisseur/{id}/receptionner` | Réceptionner → **entrées de stock** |
| `PUT /api/commandes-fournisseur/{id}/annuler` | Annuler (si encore `EN_ATTENTE`) |

**Cycle de vie :**

```
EN_ATTENTE ──receptionner──> RECUE    (irréversible, entrées de stock générées)
EN_ATTENTE ──annuler───────> ANNULEE  (aucun impact stock)
```

**Règles :**
- Les lignes figent le **prix HT** de l'article au moment de la commande.
- Code lisible `CF-000001`, `CF-000002`…
- **Réception** : seule une commande `EN_ATTENTE` peut être réceptionnée ; une
  commande `RECUE` ne peut **pas** l'être une seconde fois (le stock serait
  compté deux fois). Chaque ligne génère une **entrée de stock** (RG-03).
- Un **bon de commande** est envoyé par email au fournisseur à la création
  (RG-07, best-effort).

---

### 4.9 Ventes

Endpoints sur `/api/ventes` :

| Endpoint | Description |
|---|---|
| `GET /api/ventes` | Lister les ventes de mon entreprise |
| `GET /api/ventes/{id}` | Détail d'une vente |
| `POST /api/ventes` | Enregistrer une vente (sorties de stock immédiates) |

**Règles :**
- Une vente **décrémente le stock immédiatement** à la création : pas d'étape de
  validation, contrairement aux commandes clients.
- **Pas de modification ni de suppression** : une vente enregistrée est un fait
  historique, on ne réécrit pas l'historique des ventes.
- Le client est **optionnel** : vente anonyme possible (« Client comptoir »).
- Les lignes figent le **prix TTC** au moment de la vente.
- Code lisible `VT-000001`, `VT-000002`…
- Si une ligne manque de stock, **la vente est refusée en bloc** (rollback
  transactionnel) — pas de vente partielle.
- Chaque vente génère un mouvement de stock `SORTIE` tracé avec l'origine
  (`VT-xxxxxx`).

---

### 4.10 Stock — état, alertes, valorisation

Endpoints sur `/api/stock` (lecture seule — l'état du stock est une **vue
dérivée** des articles, pas une donnée indépendante) :

| Endpoint | Description |
|---|---|
| `GET /api/stock/etat` | État du stock : tous les articles de mon entreprise avec stock actuel et seuil |
| `GET /api/stock/alertes` | Articles dont le stock est **≤ seuil minimum** configuré |
| `GET /api/stock/valorisation` | Valeur totale du stock : Σ (stock actuel × prix unitaire HT) |

---

### 4.11 Mouvements de stock

Endpoints sur `/api/mouvements-stock` :

| Endpoint | Description |
|---|---|
| `GET /api/mouvements-stock?articleId=&type=` | Historique des mouvements (filtres optionnels par article et par type) |
| `POST /api/mouvements-stock` | Créer un **ajustement manuel** |

**Trois types de mouvement :**

| Type | Effet sur le stock | Origine |
|---|---|---|
| `ENTREE` | `stock += quantité` | Automatique — réception d'une commande fournisseur |
| `SORTIE` | `stock -= quantité` | Automatique — validation d'une commande client ou vente |
| `AJUSTEMENT` | `stock += quantité signée` (+/-) | **Manuel uniquement** — correction d'inventaire |

**Règles fondamentales (traçabilité) :**
- **Aucun code métier ne modifie `Article.stockActuel` directement** : seul le
  service des mouvements de stock en a le droit, et toujours en écrivant une
  trace `MvtStk` dans la même transaction (RG-04). Aucun changement de stock
  n'est donc possible sans historique.
- Chaque mouvement enregistre un **snapshot du stock après mouvement** :
  l'historique reste fidèle même si le stock évolue ensuite.
- Un mouvement peut porter une **origine** textuelle (ex. `CC-000042`,
  `VT-000012`) identifiant la commande ou la vente qui l'a déclenché.
- L'**ajustement manuel** exige un **motif** (RG-06) et accepte une quantité
  négative (correction à la baisse). Les entrées/sorties automatiques, elles,
  n'ont pas de motif.
- Toute opération aboutissant à un **stock négatif** est rejetée
  (`StockInsuffisantException`) et annule la transaction appelante.

---

### 4.12 Notifications in-app

| Endpoint | Description |
|---|---|
| `GET /api/notifications` | Liste des alertes actives |

Seul déclencheur implémenté (RG-09) : **stock atteignant ou passant sous le
seuil minimum**. Les alertes sont **calculées à la volée** (pas d'entité
`Notification` ni d'état lu/non-lu) : type `STOCK_BAS`, message lisible,
identifiant et désignation de l'article concerné.

---

### 4.13 Tableau de bord

| Endpoint | Description |
|---|---|
| `GET /api/dashboard/kpis` | Indicateurs clés de l'entreprise connectée |

KPIs retournés :

| KPI | Définition |
|---|---|
| Valeur du stock | Σ (stock actuel × prix HT) |
| Articles en alerte | Nombre d'articles sous leur seuil minimum |
| Commandes clients en cours | Statut `EN_COURS` |
| Commandes fournisseurs en attente | Statut `EN_ATTENTE` |
| Ventes du mois | Nombre de ventes depuis le 1er du mois |
| Chiffre d'affaires du mois | Σ des totaux des ventes du mois |

---

### 4.14 Emails automatiques

| Événement | Email | Destinataire |
|---|---|---|
| Création d'une commande client (RG-08) | Confirmation de commande (HTML) | Email du client |
| Création d'une commande fournisseur (RG-07) | Bon de commande (HTML) | Email du fournisseur |

Règles :
- Envoi **best-effort** : un échec SMTP est journalisé mais **ne bloque jamais**
  la création de la commande.
- Pas de destinataire renseigné sur la fiche → aucun envoi (et pas d'erreur).
- Configuration SMTP via les variables `MAIL_USERNAME` / `MAIL_PASSWORD`
  (vides par défaut — sans configuration, les envois échouent silencieusement).

---

## 5. Règles de gestion (récapitulatif)

| Réf. | Règle |
|---|---|
| RG-02 | La validation d'une commande client génère une sortie de stock par ligne |
| RG-03 | La réception d'une commande fournisseur génère une entrée de stock par ligne |
| RG-04 | Aucune variation de stock sans mouvement de stock tracé (`MvtStk`) |
| RG-06 | Tout ajustement manuel de stock exige un motif |
| RG-07 | Un bon de commande est envoyé par email au fournisseur à la création |
| RG-08 | Une confirmation est envoyée par email au client à la création de sa commande |
| RG-09 | Le stock atteignant le seuil minimum déclenche une notification in-app |
| — | Stock jamais négatif : toute opération qui l'entraînerait est rejetée en bloc |
| — | Prix figés (snapshot) sur les lignes de commande/vente au moment de l'opération |
| — | Codes lisibles et séquentiels : `CC-xxxxxx`, `CF-xxxxxx`, `VT-xxxxxx` |
| — | Isolation multi-tenant : les données d'une autre entreprise sont « introuvables » (404) |
| — | Transactions atomiques sur les opérations multi-lignes (création, validation, réception, vente) |
| — | Ventes immuables (pas de modification/suppression) |

---

## 6. Flux métier principaux

### 6.1 Approvisionnement (achat)

```
Création fiche fournisseur → Création commande fournisseur (EN_ATTENTE)
    → email bon de commande au fournisseur
    → Réception (PUT /receptionner) : entrées de stock ligne par ligne (RECUE)
    → Stock disponible ↑
```

### 6.2 Vente sur commande client

```
Création fiche client → Création commande client (EN_COURS)
    → email de confirmation au client
    → Validation (PUT /valider) : sorties de stock ligne par ligne (VALIDEE)
    → Stock disponible ↓
```

### 6.3 Vente au comptoir

```
Enregistrement direct de la vente (POST /api/ventes)
    → sorties de stock immédiates
    → (client facultatif)
```

### 6.4 Correction d'inventaire

```
GET /api/stock/etat (constat d'écart)
    → POST /api/mouvements-stock {articleId, quantité signée, motif}
    → stock corrigé + trace historique
```

---

## 7. Conventions techniques

- **Base de données** : schéma géré par Hibernate (`ddl-auto: update`) — les
  tables sont créées/mises à jour automatiquement au démarrage.
- **Configuration** : `application.yaml` avec variables d'environnement
  (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MS`,
  `ADMIN_LOGIN`, `ADMIN_PASSWORD`, `ADMIN_ROLE`, `CORS_ALLOWED_ORIGINS`,
  `MAIL_USERNAME`, `MAIL_PASSWORD`…). Modèle : `.env.example`.
  Les secrets **`JWT_SECRET`, `DB_PASSWORD` et `ADMIN_PASSWORD` sont requis**
  (aucune valeur par défaut) : l'application refuse de démarrer sans. Le secret
  JWT doit être une clé Base64 d'au moins 32 octets (`openssl rand -base64 48`).
- **Fichier `.env` (développement local)** : via `spring-dotenv`, un fichier
  `.env` à la racine du backend alimente ces variables sans export manuel. Il
  est **gitigné** ; copier `.env.example` vers `.env` et renseigner les
  valeurs. Les vraies variables d'environnement gardent la priorité (comportement
  inchangé en production, où le fichier est simplement absent).
- **Port serveur** : `8081`.
- **Documentation interactive** : Swagger UI sur `/swagger-ui.html`, schémas
  OpenAPI sur `/api-docs` et `/v3/api-docs` (accès public).
- **Erreurs métier** : 404 pour les ressources inexistantes ou hors entreprise,
  400 pour les règles métier violées (statut invalide, stock insuffisant…),
  403 pour les actions réservées à un rôle supérieur.

---

## 8. Hors périmètre actuel (évolutions envisageables)

Éléments explicitement non implémentés à ce jour (mentionnés dans le code) :

- **Annulation d'une commande déjà validée / reçue** (nécessiterait des
  mouvements inverses / « avoirs »).
- **Modification ou suppression d'une vente** (choix assumé : immuabilité).
- **Gestion du mot de passe** (changement, réinitialisation par email) — le
  service d'envoi d'emails existe déjà et peut servir de base.
- **Photos réelles** des articles/clients (champ présent, pas d'upload géré).
- **État lu/non-lu et historique des notifications** (calculées à la volée).
- **Templates d'emails** via moteur de template (HTML construit inline).
