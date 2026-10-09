import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { HouseholdService } from '../../../core/household/household.service';
import {
  Medication,
  MedicationForm as Form,
  MedicationRequest,
  MedicationUnit,
} from '../../../shared/models/medication';
import { DEFAULT_UNITS, FORM_LABELS, UNIT_LABELS, UNIT_SHORT_LABELS } from '../medication-labels';
import { MedicationService } from '../medication.service';

function wholeNumber(control: AbstractControl<number | null>): ValidationErrors | null {
  return control.value === null || Number.isInteger(control.value) ? null : { wholeNumber: true };
}

@Component({
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  selector: 'app-medication-editor',
  styleUrl: './medication-editor.scss',
  templateUrl: './medication-editor.html',
})
export class MedicationEditor implements OnInit {
  private readonly service = inject(MedicationService);
  private readonly household = inject(HouseholdService).current;
  private readonly router = inject(Router);

  readonly medicationId = Number(inject(ActivatedRoute).snapshot.paramMap.get('id')) || null;
  readonly forms = Object.entries(FORM_LABELS) as [Form, string][];
  readonly units = Object.entries(UNIT_LABELS) as [MedicationUnit, string][];
  readonly unitShortLabels = UNIT_SHORT_LABELS;

  readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(150)],
    }),
    activeIngredient: new FormControl('', {
      nonNullable: true,
      validators: Validators.maxLength(150),
    }),
    strength: new FormControl('', { nonNullable: true, validators: Validators.maxLength(50) }),
    form: new FormControl<Form | null>(null, Validators.required),
    unit: new FormControl<MedicationUnit | null>(null, Validators.required),
    shelfLifeAfterOpeningDays: new FormControl<number | null>(null, [
      Validators.min(1),
      wholeNumber,
    ]),
    minimumQuantity: new FormControl<number | null>(null, [
      Validators.min(0.01),
      Validators.max(99_999_999.99),
    ]),
  });

  readonly loading = signal(this.medicationId !== null);
  readonly notFound = signal(false);
  readonly busy = signal(false);
  readonly confirmingDelete = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    this.form.controls.form.valueChanges.pipe(takeUntilDestroyed()).subscribe((form) => {
      if (form && !this.form.controls.unit.dirty) {
        this.form.controls.unit.setValue(DEFAULT_UNITS[form]);
      }
    });
  }

  async ngOnInit(): Promise<void> {
    const household = this.household();
    if (!household) {
      this.error.set('Não foi possível carregar sua casa. Tente recarregar a página.');
      return;
    }
    if (this.medicationId === null) {
      return;
    }
    try {
      this.fill(await this.service.get(household.id, this.medicationId));
    } catch (error) {
      if (error instanceof HttpErrorResponse && error.status === 404) {
        this.notFound.set(true);
      } else {
        this.error.set('Não foi possível carregar o medicamento. Tente de novo.');
      }
    } finally {
      this.loading.set(false);
    }
  }

  async save(): Promise<void> {
    const household = this.household();
    if (this.form.invalid || !household) {
      this.form.markAllAsTouched();
      return;
    }
    const request = this.toRequest();
    const saved = await this.run(() =>
      this.medicationId === null
        ? this.service.create(household.id, request)
        : this.service.update(household.id, this.medicationId, request),
    );
    if (saved) {
      await this.router.navigateByUrl('/medications');
    }
  }

  async confirmDelete(): Promise<void> {
    const household = this.household();
    if (!household || this.medicationId === null) {
      return;
    }
    const id = this.medicationId;
    const deleted = await this.run(() => this.service.remove(household.id, id));
    if (deleted) {
      await this.router.navigateByUrl('/medications');
    }
  }

  private fill(medication: Medication): void {
    // emitEvent: false keeps the saved unit instead of the form's default one.
    this.form.setValue(
      {
        name: medication.name,
        activeIngredient: medication.activeIngredient ?? '',
        strength: medication.strength ?? '',
        form: medication.form,
        unit: medication.unit,
        shelfLifeAfterOpeningDays: medication.shelfLifeAfterOpeningDays,
        minimumQuantity: medication.minimumQuantity,
      },
      { emitEvent: false },
    );
  }

  private toRequest(): MedicationRequest {
    const value = this.form.getRawValue();
    return {
      name: value.name.trim(),
      activeIngredient: value.activeIngredient.trim() || null,
      strength: value.strength.trim() || null,
      form: value.form!,
      unit: value.unit!,
      shelfLifeAfterOpeningDays: value.shelfLifeAfterOpeningDays,
      minimumQuantity: value.minimumQuantity,
    };
  }

  private async run(action: () => Promise<void>): Promise<boolean> {
    this.error.set(null);
    this.busy.set(true);
    try {
      await action();
      return true;
    } catch {
      this.error.set('Algo deu errado. Tente de novo.');
      return false;
    } finally {
      this.busy.set(false);
    }
  }
}
