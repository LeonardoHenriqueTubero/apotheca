import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Household } from '../../shared/models/household';

/** App-wide: every feature (medications, storage locations...) works inside the current household. */
@Injectable({ providedIn: 'root' })
export class HouseholdService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/api/households`;

  /** `undefined` until the list has been loaded. */
  readonly households = signal<Household[] | undefined>(undefined);

  /** The household the app shows. The MVP uses the first one; switching comes with invites. */
  readonly current = computed(() => this.households()?.[0] ?? null);

  async load(): Promise<Household[]> {
    const households = await firstValueFrom(this.http.get<Household[]>(this.url));
    this.households.set(households);
    return households;
  }

  async create(name: string): Promise<Household> {
    const household = await firstValueFrom(this.http.post<Household>(this.url, { name }));
    this.households.update((households) => [...(households ?? []), household]);
    return household;
  }
}
