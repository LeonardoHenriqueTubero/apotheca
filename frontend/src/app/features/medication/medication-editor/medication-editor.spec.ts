import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';

import { registerFakeIcons } from '../../../../testing/fake-icons';
import { HouseholdService } from '../../../core/household/household.service';
import { Medication } from '../../../shared/models/medication';
import { MedicationService } from '../medication.service';
import { MedicationEditor } from './medication-editor';

describe('MedicationEditor', () => {
  let harness: RouterTestingHarness;
  let editor: MedicationEditor;
  let navigate: ReturnType<typeof vi.spyOn>;
  const service = { get: vi.fn(), create: vi.fn(), update: vi.fn(), remove: vi.fn() };

  const xarope: Medication = {
    id: 5,
    name: 'Amoxicilina',
    activeIngredient: 'amoxicilina',
    strength: '250 mg/5 ml',
    form: 'SUSPENSION',
    unit: 'G',
    shelfLifeAfterOpeningDays: 14,
    minimumQuantity: 50,
  };

  beforeEach(() => {
    service.get.mockResolvedValue(xarope);
    service.create.mockResolvedValue(undefined);
    service.update.mockResolvedValue(undefined);
    service.remove.mockResolvedValue(undefined);
  });

  afterEach(() => vi.clearAllMocks());

  async function open(url: string) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([
          { path: 'medications/new', component: MedicationEditor },
          { path: 'medications/:id', component: MedicationEditor },
        ]),
        { provide: MedicationService, useValue: service },
        { provide: HouseholdService, useValue: { current: signal({ id: 7, name: 'Casa' }) } },
      ],
    });
    registerFakeIcons('arrow_back', 'delete');
    harness = await RouterTestingHarness.create();
    editor = await harness.navigateByUrl(url, MedicationEditor);
    await harness.fixture.whenStable();
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  }

  function element() {
    return harness.routeNativeElement!;
  }

  function button(label: string) {
    return [...element().querySelectorAll<HTMLButtonElement>('button')].find((candidate) =>
      candidate.textContent?.includes(label),
    )!;
  }

  async function click(label: string) {
    button(label).click();
    await harness.fixture.whenStable();
  }

  describe('creating', () => {
    beforeEach(async () => {
      await open('/medications/new');
    });

    it('sends trimmed values, blanks as null, and goes back to the list', async () => {
      editor.form.patchValue({ name: '  Dipirona ', strength: '  ', form: 'TABLET' });

      await click('Salvar');

      expect(service.create).toHaveBeenCalledWith(7, {
        name: 'Dipirona',
        activeIngredient: null,
        strength: null,
        form: 'TABLET',
        unit: 'UNIT',
        shelfLifeAfterOpeningDays: null,
        minimumQuantity: null,
      });
      expect(navigate).toHaveBeenCalledWith('/medications');
    });

    it('suggests the unit from the form', () => {
      editor.form.controls.form.setValue('SYRUP');

      expect(editor.form.controls.unit.value).toBe('ML');
    });

    it('keeps a unit the user already chose', () => {
      editor.form.controls.unit.setValue('G');
      editor.form.controls.unit.markAsDirty();

      editor.form.controls.form.setValue('SYRUP');

      expect(editor.form.controls.unit.value).toBe('G');
    });

    it('does not send an incomplete form', async () => {
      await click('Salvar');

      expect(service.create).not.toHaveBeenCalled();
      expect(element().textContent).toContain('Digite o nome do medicamento.');
    });

    it('rejects a fractional number of days', async () => {
      editor.form.patchValue({ name: 'Colírio', form: 'DROPS', shelfLifeAfterOpeningDays: 1.5 });

      await click('Salvar');

      expect(service.create).not.toHaveBeenCalled();
    });

    it('shows an error and stays on the page when the API fails', async () => {
      service.create.mockRejectedValue(new HttpErrorResponse({ status: 500 }));
      editor.form.patchValue({ name: 'Dipirona', form: 'TABLET' });

      await click('Salvar');

      expect(navigate).not.toHaveBeenCalled();
      expect(element().querySelector('[role="alert"]')?.textContent).toContain('Algo deu errado');
    });

    it('offers no delete button', () => {
      expect(button('Excluir medicamento')).toBeUndefined();
    });
  });

  describe('editing', () => {
    it('loads the medication and keeps its saved unit', async () => {
      await open('/medications/5');

      expect(service.get).toHaveBeenCalledWith(7, 5);
      expect(editor.form.getRawValue()).toEqual({
        name: 'Amoxicilina',
        activeIngredient: 'amoxicilina',
        strength: '250 mg/5 ml',
        form: 'SUSPENSION',
        unit: 'G',
        shelfLifeAfterOpeningDays: 14,
        minimumQuantity: 50,
      });
    });

    it('saves the changes', async () => {
      await open('/medications/5');
      editor.form.patchValue({ minimumQuantity: 100 });

      await click('Salvar');

      expect(service.update).toHaveBeenCalledWith(
        7,
        5,
        expect.objectContaining({ name: 'Amoxicilina', minimumQuantity: 100 }),
      );
      expect(navigate).toHaveBeenCalledWith('/medications');
    });

    it('deletes only after confirmation', async () => {
      await open('/medications/5');

      await click('Excluir medicamento');
      expect(service.remove).not.toHaveBeenCalled();
      expect(element().textContent).toContain('apaga também as caixas');

      await click('Excluir');

      expect(service.remove).toHaveBeenCalledWith(7, 5);
      expect(navigate).toHaveBeenCalledWith('/medications');
    });

    it('says when the medication does not exist', async () => {
      service.get.mockRejectedValue(new HttpErrorResponse({ status: 404 }));

      await open('/medications/5');

      expect(element().textContent).toContain('Este medicamento não existe mais.');
      expect(element().querySelector('form')).toBeNull();
    });
  });
});
