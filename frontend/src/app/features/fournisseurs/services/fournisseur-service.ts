import { HttpClient } from "@angular/common/http";
import { Injectable, inject, signal } from "@angular/core";
import { environment } from "../../../environments/environment";
import { Fournisseur, FournisseurRequest } from "../models/fournisseur.model";

@Injectable({ providedIn: 'root' })
export class FournisseurService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiUrl}/fournisseurs`;

    private fournisseursSignal = signal<Fournisseur[]>([]);
    fournisseurs = this.fournisseursSignal.asReadonly();

    loadAll() {
        this.http.get<Fournisseur[]>(this.apiUrl)
        .subscribe(data => this.fournisseursSignal.set(data));
    }

    create(dto: FournisseurRequest) {
        return this.http.post<Fournisseur>(this.apiUrl, dto);
    }

    update(id: number, dto: FournisseurRequest) {
        return this.http.put<Fournisseur>(`${this.apiUrl}/${id}`, dto)
    }

    delete(id: number) {
        return this.http.delete<void>(`${this.apiUrl}/${id}`)
    }
}
