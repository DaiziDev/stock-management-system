import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { DashboardService } from '../services/dashboard-service';
import { AppIcon } from '../../../shared/components/icon/icon';
import {
  type ArticleStockDTO,
  type DashboardKpis,
  type VenteListe,
} from '../models/dashboard.models';

/**
 * Tableau de bord d'ENTREPRISE (ADMIN, GESTIONNAIRE, VENDEUR) : KPIs métier
 * (stock, commandes, CA du mois), activité des 7 derniers jours et listes
 * d'action (alertes de stock, dernières ventes).
 *
 * Données réelles du tenant, agrégées côté backend (/api/dashboard/kpis) et
 * complétées par les endpoints stock/ventes -- rien de codé en dur.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [DecimalPipe, RouterLink, AppIcon],
  templateUrl: './dashboard.html',
})
export class Dashboard implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  readonly kpis = signal<DashboardKpis | null>(null);
  readonly alertes = signal<ArticleStockDTO[]>([]);
  readonly ventes = signal<VenteListe[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal(false);

  /** CA par jour des 7 derniers jours (index 0 = il y a 6 jours ... 6 = aujourd'hui). */
  readonly serieCA = signal<number[]>(Array(7).fill(0));

  /** Somme des 7 jours (sous-titre du graphique). */
  readonly totalSemaine = computed(() => this.serieCA().reduce((a, b) => a + b, 0));

  /** Étiquettes du graphique : initiales des jours (L M M J V S D). */
  readonly joursSemaine = computed(() => {
    const fmt = new Intl.DateTimeFormat('fr-FR', { weekday: 'narrow' });
    const jours: string[] = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date();
      d.setDate(d.getDate() - i);
      jours.push(fmt.format(d));
    }
    return jours;
  });

  ngOnInit(): void {
    this.dashboardService.chargerTout().subscribe({
      next: ({ kpis, alertes, ventes }) => {
        this.kpis.set(kpis);
        this.alertes.set(alertes);
        this.ventes.set(ventes);
        this.serieCA.set(this.dashboardService.calculerSerieCA(ventes));
        this.chargement.set(false);
      },
      error: () => {
        this.chargement.set(false);
        this.erreur.set(true);
      },
    });
  }

  /** Hauteur (en %) de la barre du jour i pour le graphique en barres. */
  hauteurBarre(i: number): number {
    const serie = this.serieCA();
    const max = Math.max(...serie, 1);
    return Math.max((serie[i] / max) * 100, serie[i] > 0 ? 6 : 2.5);
  }

  montant(v: number | string | null | undefined): string {
    const n = Number(v ?? 0);
    return Number.isFinite(n) ? n.toLocaleString('fr-FR', { maximumFractionDigits: 0 }) + ' FCFA' : '—';
  }

  heure(iso: string): string {
    const d = new Date(iso);
    return Number.isNaN(d.getTime())
      ? '—'
      : d.toLocaleString('fr-FR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });
  }
}
