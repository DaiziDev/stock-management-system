/** KPIs renvoyés par GET /api/dashboard/kpis (BigDecimal → string en JSON). */
export interface DashboardKpis {
  valeurStock: string;
  nbArticlesEnAlerte: number;
  nbCommandesClientEnCours: number;
  nbCommandesFournisseurEnAttente: number;
  nbVentesDuMois: number;
  chiffreAffairesDuMois: string;
}

/** Article en alerte de stock (GET /api/stock/alertes). */
export interface ArticleStockDTO {
  id: number;
  codeArticle: string;
  designation: string;
  stockActuel: number;
  seuilMin: number | null;
}

/** Vente liste (GET /api/ventes) -- sous-ensemble utilisé par le dashboard. */
export interface VenteListe {
  id: number;
  code: string;
  dateVente: string;
  clientNom: string | null;
  total: string;
}
