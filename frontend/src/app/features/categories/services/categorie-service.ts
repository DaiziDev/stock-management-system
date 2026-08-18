import { HttpClient } from "@angular/common/http";
import { inject, Injectable, signal } from "@angular/core";
import { environment } from "../../../environments/environment";
import { Categorie, CategorieRequest } from "../models/categorie.model";

@Injectable({providedIn: 'root'})
export class CategorieService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiUrl}/categories`;

    private categorieSignal = signal<Categorie[]>([]);
    categories = this.categorieSignal.asReadonly();

    loadAll() {
        this.http.get<Categorie[]>(this.apiUrl)
         .subscribe(data => this.categorieSignal.set(data))
    }

    create(dto: CategorieRequest) {
        return this.http.post<Categorie>(this.apiUrl, dto)
    }

    update(id: number, dto: CategorieRequest) {
        return this.http.put<Categorie>(`${this.apiUrl}/${id}`, dto);
    }

    delete(id: number) {
        return this.http.delete<void>(`${this.apiUrl}/${id}`)
    }
}