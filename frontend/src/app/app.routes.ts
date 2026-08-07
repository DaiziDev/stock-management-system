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
];
