import { CommonModule } from "@angular/common";
import { Component, inject, OnInit, signal } from "@angular/core";
import { DashboardService } from "../services/dashboard-service";


@Component({
    selector: 'app-dashboard',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './dashboard.html'
})

export class Dashboard implements OnInit{
    private dashboardService = inject(DashboardService);

    totalCategories = this.dashboardService.totalCategories;
    totalArticles = this.dashboardService.totalArticles;

    chargement = signal(true)
    ngOnInit() {
        this.dashboardService.chargerDonnees();
        setTimeout(() => this.chargement.set(false), 600);
    }
}