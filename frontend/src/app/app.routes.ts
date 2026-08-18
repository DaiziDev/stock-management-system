import { type Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { Login } from './features/auth/login';
import { Sidebar } from './core/layout/sidebar/sidebar';

/**
 * L'application n'affiche pour l'instant que la sidebar (composant unique).
 * La route vide capture toutes les URLs (pathMatch prefix par défaut) afin que
 * les liens du menu restent actifs ; le reste des pages sera développé ensuite.
 */
export const routes: Routes = [
  { path: 'login', component: Login },
  { path: '', component: Sidebar, canActivate: [authGuard] },
  {
    path: 'categories',
    loadChildren: () =>
      import('./features/categories/categorie.routes').then((m) => m.CATEGORIE_ROUTES),
  },
  {
    path: 'articles',
    loadChildren: () => import('./features/articles/article.routes').then((m) => m.ARTICLE_ROUTES),
  },
  {
    path: 'dashboard',
    loadChildren: () =>
      import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
  },
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
];
