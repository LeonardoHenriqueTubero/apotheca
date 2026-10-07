import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';

import { HouseholdService } from '../../../core/household/household.service';

@Component({
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  selector: 'app-create-household',
  styleUrl: './create-household.scss',
  templateUrl: './create-household.html',
})
export class CreateHousehold {
  private readonly households = inject(HouseholdService);
  private readonly router = inject(Router);

  readonly form = new FormGroup({
    // `pattern(/\S/)` rejects names made only of spaces, like the API's @NotBlank.
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)],
    }),
  });
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  async submit(): Promise<void> {
    if (this.form.invalid) {
      return;
    }
    this.error.set(null);
    this.saving.set(true);
    try {
      await this.households.create(this.form.controls.name.value.trim());
      await this.router.navigateByUrl('/');
    } catch {
      this.error.set('Não foi possível criar a casa. Tente de novo.');
    } finally {
      this.saving.set(false);
    }
  }
}
