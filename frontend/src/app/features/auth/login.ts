import { Component, inject, type OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/services';
import type { UserRole } from '../../core/models/models';
import { AppIcon } from '../../shared/components/icon/icon';

@Component({
  selector: 'app-login',
  imports: [FormsModule, AppIcon],
  templateUrl: './login.html',
})
export class Login implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  email = 'admin@sgs.local';
  password = '';
  role = 'ADMIN';

  ngOnInit(): void {
    // Déjà connecté → on va directement sur le tableau de bord.
    if (this.auth.user()) {
      this.router.navigate(['/dashboard']);
    }
  }

  login(): void {
    // Mode démo : pas de vérification réelle — seul le rôle compte pour le filtre.
    this.auth.login(this.email, this.role as UserRole);

    // Retour à la page initialement demandée (paramètre redirect posé par authGuard).
    const redirect = this.route.snapshot.queryParamMap.get('redirect');
    const target = redirect && redirect.startsWith('/') ? redirect : '/dashboard';
    this.router.navigate([target]);
  }
}
