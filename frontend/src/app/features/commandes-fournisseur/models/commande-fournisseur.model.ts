/** Cycle de vie côté backend : StatutCommandeFournisseur (EN_ATTENTE -> RECUE / ANNULEE). */
export type StatutCommandeFournisseur = 'EN_ATTENTE' | 'RECUE' | 'ANNULEE';

/** Ligne d'une commande d'achat — prix en HT (on achète hors taxes). */
export interface LigneCommandeFournisseur {
  id: number;
  articleId: number;
  articleDesignation: string;
  quantite: number;
  prixUnitaire: number;
  sousTotal: number;
}

/** CommandeFournisseurResponseDTO côté backend (dateCommande en ISO). */
export interface CommandeFournisseur {
  id: number;
  code: string;
  dateCommande: string;
  statut: StatutCommandeFournisseur;
  fournisseurId: number;
  fournisseurNom: string;
  lignes: LigneCommandeFournisseur[];
  total: number;
}

/** Charge utile de POST /api/commandes-fournisseur. */
export interface CommandeFournisseurRequest {
  fournisseurId: number;
  lignes: { articleId: number; quantite: number }[];
}
