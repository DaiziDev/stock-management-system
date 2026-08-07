/**
 * Modèles du cœur de l'application (issue de la maquette maquette.html).
 * Les rôles et la structure de navigation alimentent la sidebar et les guards.
 */

export type UserRole = 'ADMIN' | 'VENDEUR' | 'GESTIONNAIRE';

export const ALL_ROLES: UserRole[] = ['ADMIN', 'VENDEUR', 'GESTIONNAIRE'];

export const ROLE_LABELS: Record<UserRole, string> = {
  ADMIN: 'Administrateur',
  VENDEUR: 'Vendeur',
  GESTIONNAIRE: 'Gestionnaire',
};

export interface CurrentUser {
  id: number;
  nom: string;
  role: UserRole;
  entrepriseId: number;
  login?: string;
}

export interface NavItem {
  key: string;
  label: string;
  icon: string;
  route: string;
  /** ⭐ Rôles autorisés à voir cet item dans le menu (et à accéder à sa route). */
  roles: UserRole[];
}

export interface NavGroup {
  group: string;
  items: NavItem[];
}

/**
 * Structure du menu — identique à la maquette (groupes + items),
 * enrichie d'un filtre `roles` pour le RBAC côté frontend.
 */
export const NAV: NavGroup[] = [
  {
    group: "Vue d'ensemble",
    items: [
      { key: 'dashboard', label: 'Tableau de bord', icon: 'layout-dashboard', route: '/dashboard', roles: ALL_ROLES },
      { key: 'stockReport', label: 'Rapports & stock', icon: 'bar-chart-3', route: '/rapports', roles: ['ADMIN', 'GESTIONNAIRE'] },
    ],
  },
  {
    group: 'Catalogue',
    items: [
      { key: 'categories', label: 'Catégories', icon: 'tag', route: '/categories', roles: ['ADMIN', 'GESTIONNAIRE'] },
      { key: 'articles', label: 'Articles', icon: 'package', route: '/articles', roles: ALL_ROLES },
    ],
  },
  {
    group: 'Organisation',
    items: [
      { key: 'entreprises', label: 'Entreprises', icon: 'building-2', route: '/entreprises', roles: ['ADMIN'] },
      { key: 'utilisateurs', label: 'Utilisateurs & rôles', icon: 'users', route: '/utilisateurs', roles: ['ADMIN'] },
    ],
  },
  {
    group: 'Tiers',
    items: [
      { key: 'clients', label: 'Clients', icon: 'user', route: '/clients', roles: ALL_ROLES },
      { key: 'fournisseurs', label: 'Fournisseurs', icon: 'truck', route: '/fournisseurs', roles: ['ADMIN', 'GESTIONNAIRE'] },
    ],
  },
  {
    group: 'Transactions',
    items: [
      { key: 'commandesClient', label: 'Commandes client', icon: 'clipboard-list', route: '/commandes-client', roles: ['ADMIN', 'GESTIONNAIRE'] },
      { key: 'commandesFournisseur', label: 'Commandes fournisseur', icon: 'clipboard-check', route: '/commandes-fournisseur', roles: ['ADMIN', 'GESTIONNAIRE'] },
      { key: 'mouvementsStock', label: 'Mouvements de stock', icon: 'arrow-left-right', route: '/mouvements-stock', roles: ['ADMIN', 'GESTIONNAIRE'] },
      { key: 'ventes', label: 'Point de vente', icon: 'shopping-cart', route: '/ventes', roles: ALL_ROLES },
    ],
  },
];
