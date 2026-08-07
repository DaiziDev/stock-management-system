import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { NAV, ROLE_LABELS } from '../../models/models';
import { AuthService } from '../../services/services';
import { AppIcon } from '../../../shared/components/icon/icon';

/**
 * ⭐ Sidebar — composant unique et autonome (un seul fichier, template et
 * styles inline). Elle est affichée seule dans l'application pour l'instant ;
 * le reste du layout sera développé progressivement.
 */
@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, AppIcon],
  template: `
    <aside class="sidebar">
      <div class="sb-brand">
        <div class="logo-mark">S</div>
        <span>SGS</span>
      </div>

      <nav class="sb-nav">
        @for (group of visibleGroups(); track group.group) {
          <div class="sb-group-label">{{ group.group }}</div>
          @for (item of group.items; track item.key) {
            <a
              class="sb-item"
              [routerLink]="item.route"
              routerLinkActive="active"
              [routerLinkActiveOptions]="{ exact: true }"
            >
              <app-icon [name]="item.icon" />
              <span>{{ item.label }}</span>
            </a>
          }
        }
      </nav>

      <div class="sb-footer">
        @if (user(); as u) {
          <div class="sb-user">
            <div class="sb-avatar">{{ u.nom.charAt(0) }}</div>
            <div class="sb-user-info">
              <b>{{ u.nom }}</b>
              <span>{{ roleLabel(u.role) }}</span>
            </div>
            <button class="sb-logout" (click)="logout()" title="Déconnexion">
              <app-icon name="log-out" />
            </button>
          </div>
        }
      </div>
    </aside>
  `,
  styles: `
    :host {
      display: block;
    }

    .sidebar {
      position: fixed;
      top: 0;
      left: 0;
      bottom: 0;
      width: var(--sbw);
      background: var(--ink-950);
      color: #cbdad5;
      display: flex;
      flex-direction: column;
      z-index: 50;
    }

    .sb-brand {
      display: flex;
      align-items: center;
      gap: 11px;
      padding: 22px 22px 16px;
    }

    .sb-brand > span {
      font-family: var(--font-d);
      font-weight: 650;
      font-size: 18px;
      color: #fff;
    }

    .sb-nav {
      flex: 1;
      overflow-y: auto;
      padding: 8px 12px;
    }

    .sb-nav::-webkit-scrollbar {
      width: 5px;
    }

    .sb-nav::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.12);
      border-radius: 3px;
    }

    .sb-group-label {
      font-family: var(--font-m);
      font-size: 10px;
      letter-spacing: 0.13em;
      text-transform: uppercase;
      color: #4e6963;
      padding: 17px 12px 7px;
    }

    .sb-item {
      position: relative;
      display: flex;
      align-items: center;
      gap: 11px;
      padding: 9.5px 12px;
      border-radius: 9px;
      font-size: 13.6px;
      font-weight: 500;
      color: #a7bab4;
      transition:
        background 0.18s var(--ease-soft),
        color 0.18s;
      margin-bottom: 2px;
    }

    .sb-item:hover {
      background: rgba(255, 255, 255, 0.055);
      color: #fff;
    }

    .sb-item.active {
      background: rgba(201, 146, 46, 0.16);
      color: #fff;
    }

    .sb-item.active::before {
      content: '';
      position: absolute;
      left: -12px;
      top: 9px;
      bottom: 9px;
      width: 3px;
      background: var(--gold-500);
      border-radius: 2px;
    }

    .sb-item app-icon {
      color: #7e9891;
      transition: color 0.18s;
    }

    .sb-item:hover app-icon {
      color: #cbdad5;
    }

    .sb-item.active app-icon {
      color: var(--gold-400);
    }

    .sb-footer {
      padding: 14px 22px 20px;
      border-top: 1px solid rgba(255, 255, 255, 0.07);
    }

    .sb-user {
      display: flex;
      align-items: center;
      gap: 10px;
    }

    .sb-avatar {
      width: 33px;
      height: 33px;
      border-radius: 50%;
      background: linear-gradient(150deg, var(--gold-400), var(--gold-600));
      color: var(--ink-950);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: 700;
      font-size: 12.5px;
      flex-shrink: 0;
    }

    .sb-user-info {
      flex: 1;
      min-width: 0;
    }

    .sb-user-info b {
      display: block;
      font-size: 13px;
      color: #fff;
    }

    .sb-user-info span {
      font-size: 11px;
      color: #6f8983;
    }

    .sb-logout {
      margin-left: auto;
      color: #6f8983;
      padding: 7px;
      border-radius: 8px;
      display: flex;
    }

    .sb-logout:hover {
      color: #fff;
      background: rgba(255, 255, 255, 0.08);
    }
  `,
})
export class Sidebar {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly groups = NAV;
  readonly user = this.auth.user;

  /**
   * ⭐ Filtre par rôle : groupes du menu recalculés selon l'utilisateur courant.
   * Les groupes dont tous les items sont interdits sont masqués.
   */
  readonly visibleGroups = computed(() =>
    this.groups
      .map((group) => ({
        ...group,
        items: group.items.filter((item) => this.auth.hasRole(...item.roles)),
      }))
      .filter((group) => group.items.length > 0)
  );

  roleLabel(role: string): string {
    return ROLE_LABELS[role as keyof typeof ROLE_LABELS] ?? role;
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
