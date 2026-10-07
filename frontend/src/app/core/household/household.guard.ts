import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { HouseholdService } from './household.service';

/** Sends users without a household to the "create your household" page. */
export const householdGuard: CanActivateFn = async () => {
  const households = inject(HouseholdService);
  const router = inject(Router);

  try {
    const list = await households.load();
    return list.length > 0 || router.createUrlTree(['/households/new']);
  } catch {
    // API unreachable: let the page open; it shows no household instead of blocking the app.
    return true;
  }
};
