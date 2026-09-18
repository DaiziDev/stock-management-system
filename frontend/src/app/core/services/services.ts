import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { environment } from '../../environments/environment';
import type { CurrentUser, LoginResponse, UserRole } from '../models/models';

/** Forme de la réponse GET /api/auth/me (CurrentUserResponse côté backend). */
interface MeResponse {
  id: number;
  nom: string;
  prenom: string;
  login: string;
  role: UserRole;
  entrepriseId: number | null;
  entrepriseNom: string | null;
}

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
          entrepriseName: response.user.entrepriseNom,
          login: response.user.login,
        };
        localStorage.setItem(TOKEN_KEY, response.token);
        localStorage.setItem(USER_KEY, JSON.stringify(user));
        this.user.set(user);
      })
    );
  }

  /**
   * Resynchronise le profil local avec le backend (GET /api/auth/me).
   *
   * Indispensable car le localStorage peut contenir un profil PÉRIMÉ : par
   * exemple le compte bootstrap promu ADMIN d'entreprise → SUPER_ADMIN par
   * le DataInitializer. Sans resynchro, l'app croit encore au vieux rôle et
   * envoie le superadmin sur l'espace entreprise au lieu de la console
   * plateforme.
   *
   * Échoue silencieusement (token expiré, backend down...) : on garde le
   * profil local et l'errorInterceptor déconnectera au prochain 401.
   */
  syncSession(): Observable<CurrentUser | null> {
    // Pas de token = pas de session à synchroniser (évite un 401 systématique
    // pour tout visiteur anonyme au chargement de l'app).
    if (!this.getToken()) {
      return of(null);
    }
    return this.http.get<MeResponse>(`${this.apiUrl}/me`).pipe(
      tap({
        next: (me) => {
          const user: CurrentUser = {
            id: me.id,
            nom: `${me.prenom} ${me.nom}`.trim(),
            role: me.role,
            entrepriseId: me.entrepriseId,
            entrepriseName: me.entrepriseNom,
            login: me.login,
          };
          this.user.set(user);
          localStorage.setItem(USER_KEY, JSON.stringify(user));
        },
        error: () => {
          /* profil local conservé ; un 401 déclenchera la déconnexion via errorInterceptor */
        },
      }),
      // La valeur retournée intéresse peu les appelants : on expose surtout
      // l'effet de bord (signal + localStorage mis à jour).
      map(() => this.user()),
      catchError(() => of(null)),
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

  /**
   * Vrai pour l'opérateur de la plateforme (SUPER_ADMIN) : il n'appartient
   * à aucune entreprise cliente et est dirigé vers la console plateforme
   * (guards + redirection post-login).
   */
  isPlatformAdmin(): boolean {
    return this.user()?.role === 'SUPER_ADMIN';
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
