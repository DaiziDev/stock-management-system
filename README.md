# SGS — Système de Gestion de Stock

SGS est une application web de gestion de stock **multi-entreprises** : chaque entreprise cliente dispose de son espace isolé (ses articles, ses clients, ses commandes), tandis qu'un opérateur de plateforme gère l'onboarding et la vue d'ensemble.

Concrètement, l'application couvre :

- **Catalogue** : articles et catégories, avec seuils d'alerte de stock ;
- **Stock** : niveaux en temps réel, mouvements (entrées, sorties, ajustements), valorisation ;
- **Achats** : commandes fournisseurs, dont la réception alimente automatiquement le stock ;
- **Ventes** : encaissement au comptoir et commandes clients, avec décrément automatique du stock ;
- **Pilotage** : tableau de bord avec KPIs et graphiques, alertes de stock, gestion des utilisateurs.

## Architecture

| Dossier     | Rôle                                                        |
|-------------|-------------------------------------------------------------|
| `backend/`  | API REST Spring Boot 3.4 (Java 17), authentification JWT    |
| `frontend/` | Application Angular 21 (SPA), Tailwind CSS, Chart.js        |
| `Docs/`     | Cahier des charges, analyse concurrente, diagrammes, maquette |

**Base de données** : PostgreSQL, schéma géré par Flyway.

## Démarrage rapide

Prérequis : **Java 17+**, **Maven 3.8+**, **Node.js 20+** (ou 22) et **PostgreSQL 14+**.

```bash
# 1. Préparer la base de données (identifiants par défaut : postgres/postgres)
sudo -u postgres psql -c "ALTER USER postgres PASSWORD 'postgres';"
sudo -u postgres createdb stock_db

# 2. Lancer le backend (port 8081)
cd backend
mvn spring-boot:run

# 3. Dans un second terminal, lancer le frontend (port 4200)
cd frontend
npm install
npm start
```

Puis ouvrez http://localhost:4200 et connectez-vous avec le compte créé automatiquement au premier démarrage du backend :

| Login             | Mot de passe | Rôle        |
|-------------------|--------------|-------------|
| `admin@sgs.local` | `admin123`   | SUPER_ADMIN |

> Note : le wrapper Maven du dépôt est incomplet, utilisez `mvn` plutôt que `./mvnw` tant qu'il n'est pas réparé.

## Documentation détaillée

- [backend/README.md](backend/README.md) : configuration, variables d'environnement, migrations Flyway, sécurité, structure du code, documentation de l'API (Swagger sur `/swagger-ui/index.html`) ;
- [frontend/README.md](frontend/README.md) : organisation des modules, conventions, scripts npm, tests.

## Rôles et espaces

L'application distingue deux espaces sous le même socle visuel :

- **Console plateforme** (`/plateforme`) : réservée au `SUPER_ADMIN`, pour onboarder les entreprises clientes et suivre les statistiques globales ;
- **Application d'entreprise** : pour `ADMIN`, `GESTIONNAIRE` et `VENDEUR`, strictement cloisonnée aux données de leur entreprise.

## Contribuer

1. Créez une branche depuis `develop` ;
2. Faites des commits clairs et ciblés ;
3. Vérifiez avant de pousser : `mvn compile` côté backend, `npm run build` côté frontend ;
4. Toute évolution du schéma de base de données passe par une migration Flyway (`V<n>__description.sql`), jamais par `ddl-auto`.
