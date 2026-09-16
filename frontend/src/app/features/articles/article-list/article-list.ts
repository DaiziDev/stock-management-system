import { CommonModule } from "@angular/common";
import { Component, computed, inject, OnInit, signal } from "@angular/core";
import { RouterLink } from "@angular/router";
import { ArticleService } from "../services/article-service";
import { AppIcon } from '../../../shared/components/icon/icon';


@Component({
    selector: 'app-article-list',
    standalone: true,
    imports: [CommonModule, RouterLink, AppIcon],
    templateUrl: './article-list.html'
})

export class ArticlieList implements OnInit {
    private articleService = inject(ArticleService);
    articles = this.articleService.articles;
    readonly loading = this.articleService.loading;
    readonly error = this.articleService.error;
    readonly recherche = signal('');
    readonly articleASupprimer = signal<number | null>(null);
    readonly articlesFiltres = computed(() => {
      const recherche = this.recherche().trim().toLocaleLowerCase();
      if (!recherche) return this.articles();
      return this.articles().filter((article) =>
        [article.codeArticle, article.designation, article.categorie?.designation]
          .some((valeur) => valeur?.toLocaleLowerCase().includes(recherche))
      );
    });

    ngOnInit() {
        this.articleService.loadAll()
    }

    ouvrirSuppression(id: number) {
        this.articleASupprimer.set(id);
    }

    annulerSuppression() {
        this.articleASupprimer.set(null);
    }

    supprimer() {
        const id = this.articleASupprimer();
        if (id === null) return;
        this.articleService.delete(id).subscribe({
          next: () => {
            this.articleASupprimer.set(null);
            this.articleService.loadAll();
          },
        });
    }
}
