import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';
import { householdGuard } from './core/household/household.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
  },
  {
    path: 'households/new',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/household/create-household/create-household').then(
        (m) => m.CreateHousehold,
      ),
  },
  {
    path: 'locations',
    canActivate: [authGuard, householdGuard],
    loadComponent: () =>
      import('./features/storage-location/storage-locations/storage-locations').then(
        (m) => m.StorageLocations,
      ),
  },
  {
    path: 'medications',
    canActivate: [authGuard, householdGuard],
    loadComponent: () =>
      import('./features/medication/medications/medications').then((m) => m.Medications),
  },
  {
    path: 'medications/new',
    canActivate: [authGuard, householdGuard],
    loadComponent: () =>
      import('./features/medication/medication-editor/medication-editor').then(
        (m) => m.MedicationEditor,
      ),
  },
  {
    path: 'medications/:id',
    canActivate: [authGuard, householdGuard],
    loadComponent: () =>
      import('./features/medication/medication-editor/medication-editor').then(
        (m) => m.MedicationEditor,
      ),
  },
  {
    path: '',
    pathMatch: 'full',
    canActivate: [authGuard, householdGuard],
    loadComponent: () => import('./features/home/home').then((m) => m.Home),
  },
  { path: '**', redirectTo: '' },
];
