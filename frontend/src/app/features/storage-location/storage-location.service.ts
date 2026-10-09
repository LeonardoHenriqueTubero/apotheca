import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { StorageLocation } from '../../shared/models/storage-location';

@Injectable({ providedIn: 'root' })
export class StorageLocationService {
  private readonly http = inject(HttpClient);

  /** `undefined` until the list has been loaded. */
  readonly locations = signal<StorageLocation[] | undefined>(undefined);

  async load(householdId: number): Promise<void> {
    this.locations.set(
      await firstValueFrom(this.http.get<StorageLocation[]>(this.url(householdId))),
    );
  }

  async create(householdId: number, name: string): Promise<void> {
    const created = await firstValueFrom(
      this.http.post<StorageLocation>(this.url(householdId), { name }),
    );
    this.locations.update((locations) => sortByName([...(locations ?? []), created]));
  }

  async rename(householdId: number, id: number, name: string): Promise<void> {
    const renamed = await firstValueFrom(
      this.http.put<StorageLocation>(`${this.url(householdId)}/${id}`, { name }),
    );
    this.locations.update((locations) =>
      sortByName((locations ?? []).map((location) => (location.id === id ? renamed : location))),
    );
  }

  async remove(householdId: number, id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${this.url(householdId)}/${id}`));
    this.locations.update((locations) =>
      (locations ?? []).filter((location) => location.id !== id),
    );
  }

  private url(householdId: number): string {
    return `${environment.apiUrl}/api/households/${householdId}/storage-locations`;
  }
}

function sortByName(locations: StorageLocation[]): StorageLocation[] {
  return [...locations].sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'));
}
