import { HttpClient } from "@angular/common/http";
import { Injectable, inject, signal } from "@angular/core";
import { environment } from "../../../environments/environment";
import { Article, ArticleRequest } from "../models/article.model";


@Injectable({ providedIn: 'root' })
export class ArticleService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiUrl}/articles`;

    private articlesSignal = signal<Article[]>([]);
    articles = this.articlesSignal.asReadonly();

    loadAll() {
        this.http.get<Article[]>(this.apiUrl)
        .subscribe(data => this.articlesSignal.set(data));
    }

    create(dto: ArticleRequest) {
        return this.http.post<Article>(this.apiUrl, dto);
    }

    update(id: number, dto: ArticleRequest) {
        return this.http.put<Article>(`${this.apiUrl}/${id}`, dto)
    }

    delete(id: number) {
        return this.http.delete<void>(`${this.apiUrl}/${id}`)
    }
}