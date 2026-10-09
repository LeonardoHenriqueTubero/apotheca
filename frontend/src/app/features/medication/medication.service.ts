import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Medication, MedicationRequest } from '../../shared/models/medication';

@Injectable({ providedIn: 'root' })
export class MedicationService {
  private readonly http = inject(HttpClient);

  /** `undefined` until the list has been loaded. */
  readonly medications = signal<Medication[] | undefined>(undefined);

  async load(householdId: number): Promise<void> {
    this.medications.set(await firstValueFrom(this.http.get<Medication[]>(this.url(householdId))));
  }

  get(householdId: number, id: number): Promise<Medication> {
    return firstValueFrom(this.http.get<Medication>(`${this.url(householdId)}/${id}`));
  }

  async create(householdId: number, request: MedicationRequest): Promise<void> {
    const created = await firstValueFrom(
      this.http.post<Medication>(this.url(householdId), request),
    );
    this.medications.update((medications) => medications && sorted([...medications, created]));
  }

  async update(householdId: number, id: number, request: MedicationRequest): Promise<void> {
    const updated = await firstValueFrom(
      this.http.put<Medication>(`${this.url(householdId)}/${id}`, request),
    );
    this.medications.update(
      (medications) =>
        medications &&
        sorted(medications.map((medication) => (medication.id === id ? updated : medication))),
    );
  }

  async remove(householdId: number, id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${this.url(householdId)}/${id}`));
    this.medications.update(
      (medications) => medications && medications.filter((medication) => medication.id !== id),
    );
  }

  private url(householdId: number): string {
    return `${environment.apiUrl}/api/households/${householdId}/medications`;
  }
}

function sorted(medications: Medication[]): Medication[] {
  return [...medications].sort(
    (a, b) =>
      a.name.localeCompare(b.name, 'pt-BR') ||
      (a.strength ?? '').localeCompare(b.strength ?? '', 'pt-BR'),
  );
}
