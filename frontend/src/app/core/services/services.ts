import { Injectable, signal } from '@angular/core';
import type { CurrentUser, UserRole } from '../models/models';

const STORAGE_KEY = 'sgs.currentUser';

/**
 * Utilisateurs de démonstration (mêmes données que la maquette).
 * À remplacer par l'appel réel à /api/auth/login (JWT) quand le backend sera prêt.
 */
const MOCK_USERS: Record<UserRole, CurrentUser> = {
  ADMIN: { id: 1, nom: 'Ravel Kamga', role: 'ADMIN', entrepriseId: 1, login: 'admin@sgs.local' },
  GESTIONNAIRE: { id: 3, nom: 'Eric Tabi', role: 'GESTIONNAIRE', entrepriseId: 1, login: 'gestionnaire@sgs.local' },
  VENDEUR: { id: 2, nom: 'Chantal Biya', role: 'VENDEUR', entrepriseId: 1, login: 'vendeur@sgs.local' },
};

@Injectable({ providedIn: 'root' })
export class AuthService {
  /** Utilisateur courant (signal) — toute l'app (sidebar, guards) réagit à ce signal. */
  readonly user = signal<CurrentUser | null>(this.restore());

  login(email: string, role: UserRole): void {
    const user: CurrentUser = { ...MOCK_USERS[role], login: email };
    this.user.set(user);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
  }

  logout(): void {
    this.user.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  /** ⭐ Vérifie si l'utilisateur courant possède l'un des rôles donnés. */
  hasRole(...roles: UserRole[]): boolean {
    const role = this.user()?.role;
    return !!role && roles.includes(role);
  }

  private restore(): CurrentUser | null {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return raw ? (JSON.parse(raw) as CurrentUser) : null;
    } catch {
      return null;
    }
  }
}
