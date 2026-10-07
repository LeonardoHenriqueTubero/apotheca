import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { HouseholdService } from '../../../core/household/household.service';
import { CreateHousehold } from './create-household';

describe('CreateHousehold', () => {
  let fixture: ComponentFixture<CreateHousehold>;
  let create: ReturnType<typeof vi.fn>;
  let navigate: ReturnType<typeof vi.spyOn>;

  beforeEach(async () => {
    create = vi.fn().mockResolvedValue({ id: 1, name: 'Casa da Maria', role: 'OWNER' });
    await TestBed.configureTestingModule({
      imports: [CreateHousehold],
      providers: [provideRouter([]), { provide: HouseholdService, useValue: { create } }],
    }).compileComponents();
    navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    fixture = TestBed.createComponent(CreateHousehold);
    await fixture.whenStable();
  });

  function element() {
    return fixture.nativeElement as HTMLElement;
  }

  async function submit(name: string) {
    const input = element().querySelector('input')!;
    input.value = name;
    input.dispatchEvent(new Event('input'));
    element().querySelector<HTMLButtonElement>('button[type="submit"]')!.click();
    await fixture.whenStable();
  }

  it('creates the household with a trimmed name and goes home', async () => {
    await submit('  Casa da Maria  ');

    expect(create).toHaveBeenCalledWith('Casa da Maria');
    expect(navigate).toHaveBeenCalledWith('/');
  });

  it('does not send a blank name and asks for one', async () => {
    await submit('   ');

    expect(create).not.toHaveBeenCalled();
    expect(element().querySelector('mat-error')?.textContent).toContain('Digite um nome');
  });

  it('shows a message when the API fails', async () => {
    create.mockRejectedValue(new Error('offline'));

    await submit('Casa da Maria');

    expect(navigate).not.toHaveBeenCalled();
    expect(element().querySelector('[role="alert"]')?.textContent).toContain(
      'Não foi possível criar a casa',
    );
  });
});
