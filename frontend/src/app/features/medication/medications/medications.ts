import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { HouseholdService } from '../../../core/household/household.service';
import { FORM_LABELS } from '../medication-labels';
import { MedicationService } from '../medication.service';

@Component({
  imports: [RouterLink, MatButtonModule, MatIconModule],
  selector: 'app-medications',
  styleUrl: './medications.scss',
  templateUrl: './medications.html',
})
export class Medications implements OnInit {
  private readonly service = inject(MedicationService);
  private readonly household = inject(HouseholdService).current;

  readonly medications = this.service.medications;
  readonly formLabels = FORM_LABELS;
  readonly error = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    const household = this.household();
    if (!household) {
      this.error.set('Não foi possível carregar sua casa. Tente recarregar a página.');
      return;
    }
    try {
      await this.service.load(household.id);
    } catch {
      this.error.set('Não foi possível carregar os medicamentos. Tente de novo.');
    }
  }
}
