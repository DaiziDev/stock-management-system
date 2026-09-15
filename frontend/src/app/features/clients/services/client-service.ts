import { HttpClient } from "@angular/common/http";
import { Injectable, inject, signal } from "@angular/core";
import { environment } from "../../../environments/environment";
import { Client, ClientRequest } from "../models/client.model";

@Injectable({ providedIn: 'root' })
export class ClientService {
    private http = inject(HttpClient);
    private apiUrl = `${environment.apiUrl}/clients`;

    private clientsSignal = signal<Client[]>([]);
    clients = this.clientsSignal.asReadonly();

    loadAll() {
        this.http.get<Client[]>(this.apiUrl)
        .subscribe(data => this.clientsSignal.set(data));
    }

    create(dto: ClientRequest) {
        return this.http.post<Client>(this.apiUrl, dto);
    }

    update(id: number, dto: ClientRequest) {
        return this.http.put<Client>(`${this.apiUrl}/${id}`, dto)
    }

    delete(id: number) {
        return this.http.delete<void>(`${this.apiUrl}/${id}`)
    }
}
