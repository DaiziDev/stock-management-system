import { computed, inject, Injectable } from "@angular/core";
import { CategorieService } from "../../categories/services/categorie-service";
import { ArticleService } from "../../articles/services/article-service";

@Injectable({providedIn: 'root'})
export class DashboardService {
    private categorieService = inject(CategorieService);
    private articleService = inject(ArticleService);

    totalCategories = computed(() => this.categorieService.categories().length);
    totalArticles = computed(() => this.articleService.articles().length);

    chargerDonnees() {
        this.categorieService.loadAll();
        this.articleService.loadAll();
    }
}