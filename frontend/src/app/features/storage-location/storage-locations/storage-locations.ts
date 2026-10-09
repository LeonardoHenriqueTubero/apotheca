import { HttpErrorResponse } from '@angular/common/http';
import {
  Component,
  ElementRef,
  Injector,
  OnInit,
  afterNextRender,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import {
  FormControl,
  FormGroup,
  FormGroupDirective,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';

import { HouseholdService } from '../../../core/household/household.service';
import { StorageLocation } from '../../../shared/models/storage-location';
import { StorageLocationService } from '../storage-location.service';

const SUGGESTIONS = ['Armário do banheiro', 'Cozinha', 'Quarto', 'Bolsa'];

function nameForm() {
  return new FormGroup({
    // `pattern(/\S/)` rejects names made only of spaces, like the API's @NotBlank.
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.pattern(/\S/), Validators.maxLength(100)],
    }),
  });
}

@Component({
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  selector: 'app-storage-locations',
  styleUrl: './storage-locations.scss',
  templateUrl: './storage-locations.html',
})
export class StorageLocations implements OnInit {
  private readonly service = inject(StorageLocationService);
  private readonly injector = inject(Injector);

  readonly household = inject(HouseholdService).current;
  readonly locations = this.service.locations;
  readonly suggestions = SUGGESTIONS;

  readonly addForm = nameForm();
  readonly editForm = nameForm();
  private readonly addFormDirective = viewChild.required<FormGroupDirective>('addFormRef');
  private readonly editInput = viewChild<ElementRef<HTMLInputElement>>('editInput');
  readonly editingId = signal<number | null>(null);
  readonly confirmingDeleteId = signal<number | null>(null);
  readonly busy = signal(false);
  readonly error = signal<string | null>(null);

  async ngOnInit(): Promise<void> {
    const household = this.household();
    if (household) {
      await this.run(() => this.service.load(household.id));
    } else {
      this.error.set('Não foi possível carregar sua casa. Tente recarregar a página.');
    }
  }

  async add(name = this.addForm.controls.name.value): Promise<void> {
    if (!name.trim()) {
      this.addForm.markAllAsTouched();
      return;
    }
    const added = await this.runInHousehold(
      (householdId) => this.service.create(householdId, name.trim()),
      'Já existe um local com esse nome.',
    );
    if (added) {
      // reset() alone keeps the form "submitted", so the empty field would show its error.
      this.addFormDirective().resetForm();
    }
  }

  startEditing(location: StorageLocation): void {
    this.confirmingDeleteId.set(null);
    this.editForm.setValue({ name: location.name });
    this.editingId.set(location.id);
    afterNextRender(
      () => {
        const input = this.editInput()?.nativeElement;
        input?.focus();
        input?.select();
      },
      { injector: this.injector },
    );
  }

  async saveEdit(location: StorageLocation): Promise<void> {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }
    const saved = await this.runInHousehold(
      (householdId) =>
        this.service.rename(householdId, location.id, this.editForm.controls.name.value.trim()),
      'Já existe um local com esse nome.',
    );
    if (saved) {
      this.editingId.set(null);
    }
  }

  askToDelete(location: StorageLocation): void {
    this.editingId.set(null);
    this.confirmingDeleteId.set(location.id);
  }

  async confirmDelete(location: StorageLocation): Promise<void> {
    await this.runInHousehold(
      (householdId) => this.service.remove(householdId, location.id),
      'Este local ainda guarda medicamentos. Mova-os antes de excluir.',
    );
    this.confirmingDeleteId.set(null);
  }

  private runInHousehold(
    action: (householdId: number) => Promise<void>,
    conflictMessage: string,
  ): Promise<boolean> {
    const household = this.household();
    return household
      ? this.run(() => action(household.id), conflictMessage)
      : Promise.resolve(false);
  }

  /** Runs an API call; a 409 shows `conflictMessage`, anything else a generic error. */
  private async run(action: () => Promise<void>, conflictMessage?: string): Promise<boolean> {
    this.error.set(null);
    this.busy.set(true);
    try {
      await action();
      return true;
    } catch (error) {
      const conflict = error instanceof HttpErrorResponse && error.status === 409;
      this.error.set((conflict && conflictMessage) || 'Algo deu errado. Tente de novo.');
      return false;
    } finally {
      this.busy.set(false);
    }
  }
}
