import { HttpClient } from "@angular/common/http";
import { Injectable, inject, signal } from "@angular/core";
import { catchError, finalize, throwError } from 'rxjs';
import { environment } from "../../../environments/environment";
import { Article, ArticleRequest } from "../models/article.model";


@Injectable({ providedIn: 'root' })
export class ArticleService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiUrl}/articles`;

    private articlesSignal = signal<Article[]>([]);
    articles = this.articlesSignal.asReadonly();
    readonly loading = signal(false);
    readonly error = signal<string | null>(null);

    loadAll() {
        this.loading.set(true);
        this.error.set(null);
        this.http.get<Article[]>(this.apiUrl)
        .pipe(
            finalize(() => this.loading.set(false)),
            catchError((error) => {
                this.error.set('Impossible de charger les articles. Réessaie dans un instant.');
                return throwError(() => error);
            })
        )
        .subscribe({ next: (data) => this.articlesSignal.set(data) });
    }

    findById(id: number) {
        return this.http.get<Article>(`${this.apiUrl}/${id}`);
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
