import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { registerFakeIcons } from '../../../../testing/fake-icons';
import { HouseholdService } from '../../../core/household/household.service';
import { Medication } from '../../../shared/models/medication';
import { MedicationService } from '../medication.service';
import { Medications } from './medications';

describe('Medications', () => {
  let fixture: ComponentFixture<Medications>;
  const medications = signal<Medication[] | undefined>(undefined);
  const load = vi.fn();

  const dipirona: Medication = {
    id: 1,
    name: 'Dipirona',
    activeIngredient: 'dipirona monoidratada',
    strength: '500 mg',
    form: 'TABLET',
    unit: 'UNIT',
    shelfLifeAfterOpeningDays: null,
    minimumQuantity: 10,
  };

  async function render(list: Medication[], household: unknown = { id: 7, name: 'Casa' }) {
    medications.set(list);
    await TestBed.configureTestingModule({
      imports: [Medications],
      providers: [
        provideRouter([]),
        { provide: MedicationService, useValue: { medications, load } },
        { provide: HouseholdService, useValue: { current: signal(household) } },
      ],
    }).compileComponents();
    registerFakeIcons('add', 'arrow_back');

    fixture = TestBed.createComponent(Medications);
    await fixture.whenStable();
  }

  beforeEach(() => {
    load.mockResolvedValue(undefined);
  });
  afterEach(() => vi.clearAllMocks());

  function element() {
    return fixture.nativeElement as HTMLElement;
  }

  it('loads the medications of the current household', async () => {
    await render([dipirona]);

    expect(load).toHaveBeenCalledWith(7);
  });

  it('shows name, strength and form, linking to the edit page', async () => {
    await render([dipirona]);

    const link = element().querySelector('li a')!;
    expect(link.querySelector('.name')?.textContent).toContain('Dipirona');
    expect(link.querySelector('.strength')?.textContent).toBe('500 mg');
    expect(link.querySelector('.form')?.textContent).toBe('Comprimido');
    expect(link.getAttribute('href')).toBe('/medications/1');
  });

  it('says when there are no medications yet', async () => {
    await render([]);

    expect(element().querySelector('.empty')?.textContent).toContain('Nenhum medicamento');
  });

  it('shows an error when the list cannot be loaded', async () => {
    load.mockRejectedValue(new Error('offline'));

    await render([]);

    expect(element().querySelector('[role="alert"]')?.textContent).toContain(
      'Não foi possível carregar os medicamentos',
    );
  });

  it('does not call the API without a household', async () => {
    await render([], null);

    expect(load).not.toHaveBeenCalled();
    expect(element().querySelector('[role="alert"]')?.textContent).toContain('sua casa');
  });
});
