import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Router } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { HouseholdService } from '../../core/household/household.service';

@Component({
  imports: [MatButtonModule, MatIconModule],
  selector: 'app-home',
  styleUrl: './home.scss',
  templateUrl: './home.html',
})
export class Home {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly household = inject(HouseholdService).current;

  readonly firstName = computed(() => {
    const name = this.auth.profile()?.displayName ?? this.auth.user()?.displayName ?? '';
    return name.split(' ')[0];
  });

  async signOut(): Promise<void> {
    await this.auth.signOut();
    await this.router.navigateByUrl('/login');
  }
}
