import { Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';

import { AuthService } from '../../../core/auth/auth.service';

const ERROR_MESSAGES: Record<string, string> = {
  'auth/popup-closed-by-user': 'O login foi cancelado.',
  'auth/cancelled-popup-request': 'O login foi cancelado.',
  'auth/popup-blocked':
    'O navegador bloqueou a janela de login. Permita pop-ups para este site e tente de novo.',
  'auth/network-request-failed': 'Sem conexão com a internet. Tente de novo.',
};

@Component({
  imports: [MatButtonModule, MatIconModule],
  selector: 'app-login',
  styleUrl: './login.scss',
  templateUrl: './login.html',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly signingIn = signal(false);
  readonly error = signal<string | null>(null);

  async signIn(): Promise<void> {
    this.error.set(null);
    this.signingIn.set(true);
    try {
      await this.auth.signInWithGoogle();
      await this.router.navigateByUrl('/');
    } catch (error) {
      const code = (error as { code?: string } | null)?.code ?? '';
      this.error.set(ERROR_MESSAGES[code] ?? 'Não foi possível entrar. Tente de novo.');
    } finally {
      this.signingIn.set(false);
    }
  }
}
