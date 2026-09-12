import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/services';

/**
 * Si le backend répond 401 (token absent/expiré/invalide), l'utilisateur est
 * déconnecté et renvoyé vers /login — sinon l'app resterait dans un état
 * incohérent : "connecté" côté frontend (signal `user` toujours présent)
 * mais rejeté par chaque appel API.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error) => {
      if (error?.status === 401 && !req.url.includes('/auth/login')) {
        auth.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    })
  );
};
