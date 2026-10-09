import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../auth/auth.service';
import { HouseholdService } from './household.service';

/** Sends users without a household to the "create your household" page. */
export const householdGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const households = inject(HouseholdService);
  const router = inject(Router);

  // Angular runs a route's guards at the same time, so this one must wait for Firebase to
  // restore the session itself; otherwise the request below leaves without a token (401).
  if (!(await auth.isSignedIn())) {
    return true;
  }

  try {
    const list = await households.load();
    return list.length > 0 || router.createUrlTree(['/households/new']);
  } catch {
    // API unreachable: let the page open; it shows no household instead of blocking the app.
    return true;
  }
};
