import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';

import { Household } from '../../shared/models/household';
import { AuthService } from '../auth/auth.service';
import { householdGuard } from './household.guard';
import { HouseholdService } from './household.service';

describe('householdGuard', () => {
  function runGuard(load: () => Promise<Household[]>, isSignedIn = () => Promise.resolve(true)) {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: HouseholdService, useValue: { load } },
        { provide: AuthService, useValue: { isSignedIn } },
      ],
    });
    return TestBed.runInInjectionContext(() =>
      householdGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    );
  }

  it('lets a user with a household through', async () => {
    const result = await runGuard(() => Promise.resolve([{ id: 1, name: 'Casa', role: 'OWNER' }]));

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

  it('waits for the restored session before asking the API', async () => {
    const events: string[] = [];
    let finishRestoring!: (signedIn: boolean) => void;
    const restoring = new Promise<boolean>((resolve) => (finishRestoring = resolve));

    const guard = runGuard(
      () => {
        events.push('load');
        return Promise.resolve([{ id: 1, name: 'Casa', role: 'OWNER' }]);
      },
      () => restoring,
    );
    events.push('restored');
    finishRestoring(true);
    await guard;

    expect(events).toEqual(['restored', 'load']);
  });

  it('leaves signed-out users to the auth guard without calling the API', async () => {
    const load = vi.fn();

    const result = await runGuard(load, () => Promise.resolve(false));

    expect(result).toBe(true);
    expect(load).not.toHaveBeenCalled();
  });
});
