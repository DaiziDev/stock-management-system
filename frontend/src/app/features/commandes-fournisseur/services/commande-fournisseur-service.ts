import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { type CommandeFournisseur, type CommandeFournisseurRequest } from '../models/commande-fournisseur.model';

/**
 * Accès API du module commandes fournisseur. Pas de cache partagé :
 * la liste et le détail rechargent leur donnée — une commande peut être
 * réceptionnée depuis un autre onglet, on affiche toujours du frais.
 */
@Injectable({ providedIn: 'root' })
export class CommandeFournisseurService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/commandes-fournisseur`;

  loadAll(): Observable<CommandeFournisseur[]> {
    return this.http.get<CommandeFournisseur[]>(this.apiUrl);
  }

  findById(id: number): Observable<CommandeFournisseur> {
    return this.http.get<CommandeFournisseur>(`${this.apiUrl}/${id}`);
  }

  /**
   * Crée la commande d'achat + ses lignes en une requête ; le backend fixe
   * le code (CF-000042), le statut EN_ATTENTE et envoie le bon de commande
   * au fournisseur par email (RG-07).
   */
  creer(dto: CommandeFournisseurRequest): Observable<CommandeFournisseur> {
    return this.http.post<CommandeFournisseur>(this.apiUrl, dto);
  }

  /**
   * EN_ATTENTE -> RECUE : le backend génère une ENTRÉE de stock par ligne.
   * Une commande déjà reçue est refusée (le stock serait compté deux fois).
   */
  receptionner(id: number): Observable<CommandeFournisseur> {
    return this.http.put<CommandeFournisseur>(`${this.apiUrl}/${id}/receptionner`, {});
  }

  /** EN_ATTENTE -> ANNULEE, sans impact stock. */
  annuler(id: number): Observable<CommandeFournisseur> {
    return this.http.put<CommandeFournisseur>(`${this.apiUrl}/${id}/annuler`, {});
  }
}
