import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';

import { Household } from '../../shared/models/household';
import { householdGuard } from './household.guard';
import { HouseholdService } from './household.service';

describe('householdGuard', () => {
  function runGuard(load: () => Promise<Household[]>) {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: HouseholdService, useValue: { load } }],
    });
    return TestBed.runInInjectionContext(() =>
      householdGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
  }

  it('lets a user with a household through', async () => {
    const result = await runGuard(() =>
      Promise.resolve([{ id: 1, name: 'Casa', role: 'OWNER' }]),
    );

    expect(result).toBe(true);
  });

  it('sends a user without a household to the create page', async () => {
    const result = await runGuard(() => Promise.resolve([]));

    expect(result).toBeInstanceOf(UrlTree);
    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/households/new');
  });

  it('does not block the app when the API fails', async () => {
    const result = await runGuard(() => Promise.reject(new Error('offline')));

    expect(result).toBe(true);
  });
});
