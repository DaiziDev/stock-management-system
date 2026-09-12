import { Component, inject, type OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/services';
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

  identifiant = 'admin@sgs.local';
  password = '';
  loading = false;
  error: string | null = null;

  ngOnInit(): void {
    // Déjà connecté → on va directement sur le tableau de bord.
    if (this.auth.user()) {
      this.router.navigate(['/dashboard']);
    }
  }

  submit(): void {
    if (this.loading) return;
    this.error = null;
    this.loading = true;

    this.auth.login(this.identifiant, this.password).subscribe({
      next: () => {
        // Retour à la page initialement demandée (paramètre redirect posé par authGuard).
        const redirect = this.route.snapshot.queryParamMap.get('redirect');
        const target = redirect && redirect.startsWith('/') ? redirect : '/dashboard';
        this.router.navigate([target]);
      },
      error: () => {
        this.loading = false;
        this.error = 'Identifiant ou mot de passe incorrect.';
      },
    });
  }
}
