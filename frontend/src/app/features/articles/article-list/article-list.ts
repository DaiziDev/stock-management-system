import { CommonModule } from "@angular/common";
import { Component, inject, OnInit } from "@angular/core";
import { RouterLink } from "@angular/router";
import { ArticleService } from "../services/article-service";


@Component({
    selector: 'app-article-list',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './article-list.html'
})

export class ArticlieList implements OnInit {
    private articleService = inject(ArticleService);
    articles = this.articleService.articles;

    ngOnInit() {
        this.articleService.loadAll()
    }

    supprimer(id: number) {
        this.articleService.delete(id).subscribe(() => this.articleService.loadAll())
    }
}