import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { registerFakeIcons } from '../../../../testing/fake-icons';
import { HouseholdService } from '../../../core/household/household.service';
import { StorageLocation } from '../../../shared/models/storage-location';
import { StorageLocationService } from '../storage-location.service';
import { StorageLocations } from './storage-locations';

describe('StorageLocations', () => {
  let fixture: ComponentFixture<StorageLocations>;
  const locations = signal<StorageLocation[] | undefined>(undefined);
  const service = {
    locations,
    load: vi.fn(),
    create: vi.fn(),
    rename: vi.fn(),
    remove: vi.fn(),
  };

  async function render(
    initial: StorageLocation[],
    household: unknown = { id: 7, name: 'Casa', role: 'OWNER' },
  ) {
    locations.set(initial);
    service.load.mockResolvedValue(undefined);
    service.create.mockResolvedValue(undefined);
    service.rename.mockResolvedValue(undefined);
    service.remove.mockResolvedValue(undefined);
    await TestBed.configureTestingModule({
      imports: [StorageLocations],
      providers: [
        provideRouter([]),
        { provide: StorageLocationService, useValue: service },
        {
          provide: HouseholdService,
          useValue: { current: signal(household) },
        },
      ],
    }).compileComponents();
    registerFakeIcons('add', 'arrow_back', 'delete', 'edit');

    fixture = TestBed.createComponent(StorageLocations);
    await fixture.whenStable();
  }

  afterEach(() => vi.clearAllMocks());

  function element() {
    return fixture.nativeElement as HTMLElement;
  }

  function button(label: string) {
    return [...element().querySelectorAll<HTMLButtonElement>('button')].find(
      (candidate) =>
        candidate.textContent?.trim() === label || candidate.getAttribute('aria-label') === label,
    )!;
  }

  async function click(label: string) {
    button(label).click();
    await fixture.whenStable();
  }

  async function type(text: string, input = element().querySelector('input')!) {
    input.value = text;
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
  }

  function alertText() {
    return element().querySelector('[role="alert"]')?.textContent;
  }

  it('loads the locations of the current household', async () => {
    await render([{ id: 1, name: 'Bolsa' }]);

    expect(service.load).toHaveBeenCalledWith(7);
    expect(element().querySelector('li')?.textContent).toContain('Bolsa');
  });

  it('suggests common locations when there are none and adds the chosen one', async () => {
    await render([]);

    await click('Cozinha');

    expect(service.create).toHaveBeenCalledWith(7, 'Cozinha');
  });

  it('adds a typed location without surrounding spaces', async () => {
    await render([]);

    await type('  Mochila  ');
    await click('Adicionar');

    expect(service.create).toHaveBeenCalledWith(7, 'Mochila');
  });

  it('clears the field without showing an error after adding', async () => {
    await render([]);

    await type('Mochila');
    await click('Adicionar');

    expect(element().querySelector('input')!.value).toBe('');
    expect(element().querySelector('mat-error')).toBeNull();
  });

  it('explains a duplicate name', async () => {
    await render([{ id: 1, name: 'Bolsa' }]);
    service.create.mockRejectedValue(new HttpErrorResponse({ status: 409 }));

    await type('bolsa');
    await click('Adicionar');

    expect(alertText()).toContain('Já existe um local com esse nome.');
  });

  it('focuses the name field with its text selected when renaming starts', async () => {
    await render([{ id: 1, name: 'Banheiro' }]);

    await click('Renomear Banheiro');

    const input = element().querySelector<HTMLInputElement>('li input')!;
    expect(document.activeElement).toBe(input);
    expect(input.selectionEnd! - input.selectionStart!).toBe('Banheiro'.length);
  });

  it('renames a location', async () => {
    await render([{ id: 1, name: 'Banheiro' }]);

    await click('Renomear Banheiro');
    await type('Armário do banheiro', element().querySelector<HTMLInputElement>('li input')!);
    await click('Salvar');

    expect(service.rename).toHaveBeenCalledWith(7, 1, 'Armário do banheiro');
  });

  it('asks for confirmation before deleting', async () => {
    await render([{ id: 1, name: 'Bolsa' }]);

    await click('Excluir Bolsa');
    expect(service.remove).not.toHaveBeenCalled();
    expect(element().querySelector('li')?.textContent).toContain('Excluir “Bolsa”?');

    await click('Excluir');

    expect(service.remove).toHaveBeenCalledWith(7, 1);
  });

  it('explains why a location with medicines cannot be deleted', async () => {
    await render([{ id: 1, name: 'Bolsa' }]);
    service.remove.mockRejectedValue(new HttpErrorResponse({ status: 409 }));

    await click('Excluir Bolsa');
    await click('Excluir');

    expect(alertText()).toContain('Este local ainda guarda medicamentos.');
  });

  it('says so when the household could not be loaded', async () => {
    await render([], null);

    expect(service.load).not.toHaveBeenCalled();
    expect(alertText()).toContain('Não foi possível carregar sua casa.');
  });

  it('shows a generic message for other errors', async () => {
    await render([]);
    service.create.mockRejectedValue(new HttpErrorResponse({ status: 500 }));

    await click('Bolsa');

    expect(alertText()).toContain('Algo deu errado. Tente de novo.');
  });
});
