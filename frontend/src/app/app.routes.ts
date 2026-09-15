import { type Routes } from '@angular/router';
import { authGuard } from './core/guards/auth-guard';
import { Login } from './features/auth/login';

/**
 * Toutes les routes applicatives (hors /login) sont enfants d'une route vide
 * gardée par authGuard : ça protège chaque fonctionnalité en un seul endroit,
 * plutôt que de devoir répéter `canActivate: [authGuard]` sur chacune (et
 * risquer d'en oublier une, comme c'était le cas avant).
 */
export const routes: Routes = [
  { path: 'login', component: Login },
  {
    path: '',
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'categories',
        loadChildren: () =>
          import('./features/categories/categorie.routes').then((m) => m.CATEGORIE_ROUTES),
      },
      {
        path: 'articles',
        loadChildren: () =>
          import('./features/articles/article.routes').then((m) => m.ARTICLE_ROUTES),
      },
      {
        path: 'dashboard',
        loadChildren: () =>
          import('./features/dashboard/dashboard.routes').then((m) => m.DASHBOARD_ROUTES),
      },
      {
        path: 'entreprises',
        loadChildren: () =>
          import('./features/entreprises/entreprise.routes').then((m) => m.ENTREPRISE_ROUTES),
      },
      {
        path: 'clients',
        loadChildren: () =>
          import('./features/clients/client.routes').then((m) => m.CLIENT_ROUTES),
      },
      {
        path: 'fournisseurs',
        loadChildren: () =>
          import('./features/fournisseurs/fournisseur.routes').then((m) => m.FOURNISSEUR_ROUTES),
      },
    ],
  },
];
