import { Component, inject } from '@angular/core';
import { SidebarService } from '../layout.service';
import { AppIcon } from '../../../shared/components/icon/icon';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [AppIcon],
  template: `
    <nav class="topbar">
      <button
        type="button"
        class="tb-hamburger"
        (click)="toggleSidebar()"
        aria-label="Menu"
      >
        <app-icon name="menu" />
      </button>

      <div class="tb-search">
        <app-icon name="search" />
        <input type="text" placeholder="Rechercher..." />
      </div>

      <div class="tb-spacer"></div>

      <div class="flex items-center gap-3">
        <button type="button" class="tb-entreprise">
          <span class="dot"></span>
          Bafoussam Trading Co
          <app-icon name="chevron-down" class="chev" />
        </button>

        <button
          type="button"
          aria-label="Notifications"
          class="tb-icon-btn"
        >
          <app-icon name="bell" />
          <span class="tb-badge"></span>
        </button>
      </div>
    </nav>
  `,
})
export class Navbar {
  private readonly sidebar = inject(SidebarService);

  toggleSidebar(): void {
    this.sidebar.toggle();
  }
}
