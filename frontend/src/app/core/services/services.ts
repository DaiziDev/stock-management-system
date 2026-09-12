import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import type { CurrentUser, LoginResponse, UserRole } from '../models/models';

const TOKEN_KEY = 'sgs.token';
const USER_KEY = 'sgs.currentUser';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/auth`;

  /** Utilisateur courant (signal) — toute l'app (sidebar, guards) réagit à ce signal. */
  readonly user = signal<CurrentUser | null>(this.restoreUser());

  /**
   * Authentifie l'utilisateur via POST /api/auth/login.
   * En cas de succès : stocke le JWT + le profil, et met à jour le signal `user`.
   * En cas d'échec (401/403) : l'appelant reçoit l'erreur (voir Login.submit()).
   */
  login(login: string, motDePasse: string): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.apiUrl}/login`, { login, motDePasse }).pipe(
      tap((response) => {
        const user: CurrentUser = {
          id: response.user.id,
          nom: `${response.user.prenom} ${response.user.nom}`.trim(),
          role: response.user.role,
          entrepriseId: response.user.entrepriseId,
          login: response.user.login,
        };
        localStorage.setItem(TOKEN_KEY, response.token);
        localStorage.setItem(USER_KEY, JSON.stringify(user));
        this.user.set(user);
      })
    );
  }

  logout(): void {
    this.user.set(null);
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  /** Lu par l'intercepteur HTTP pour poser le header Authorization. */
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  /** ⭐ Vérifie si l'utilisateur courant possède l'un des rôles donnés. */
  hasRole(...roles: UserRole[]): boolean {
    const role = this.user()?.role;
    return !!role && roles.includes(role);
  }

  private restoreUser(): CurrentUser | null {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? (JSON.parse(raw) as CurrentUser) : null;
    } catch {
      return null;
    }
  }
}
