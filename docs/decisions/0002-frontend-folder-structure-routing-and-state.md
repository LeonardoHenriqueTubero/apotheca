# ADR 0002: Frontend folder structure, routing and state management

- **Status:** Accepted
- **Date:** 2026-09-26
- **Decision log:** #27, #28, #29

## Context

The Angular frontend has no code yet. Before generating the skeleton we need to
decide how folders are organized, how routes are loaded, and where application
state lives.

This ADR is about code organization only. The UI library is decided in ADR 0003;
the visual identity is still an open decision.

The priorities are the same as in ADR 0001: a structure that recruiters recognize,
that is easy to explain in an interview, and that does not add concepts the MVP
does not need. The app is used mostly on phones, so the initial download should
stay small.

## Decision

### 1. Organization by feature, with `core/`, `shared/` and `features/`

The official Angular style guide recommends organizing code by feature area instead
of by type of file. On top of that we use `core/` and `shared/`, a widely used
community convention (it comes from the older style guide's `CoreModule` and
`SharedModule`):

```
frontend/src/app
├── core
├── shared
│   ├── components
│   ├── pipes
│   └── models
├── features
│   ├── auth
│   ├── household
│   └── medication
│       ├── medication-list/
│       ├── medication-form/
│       └── medication.service.ts
├── app.routes.ts
├── app.config.ts
└── app.ts
```

| Folder | Contents |
|---|---|
| `core` | App-wide singletons: auth guard, JWT interceptor, services used by the whole app |
| `shared` | Reusable components and pipes with no business logic (e.g. an expiry status badge) |
| `shared/models` | TypeScript interfaces that mirror the backend DTOs |
| `features` | One folder per domain (`medication`, `household`, `auth`...), each with its own components and feature-specific service |

Dependency rules:

- A feature may import from `core` and `shared`.
- A feature never imports from another feature. If two features need the same
  thing, it moves to `shared` (UI) or `core` (app-wide service).
- `shared` imports nothing from `core` or `features`.

### 2. Routing in `app.routes.ts`, lazy loaded with `loadComponent`

All routes are declared in `app.routes.ts`. Feature pages are lazy loaded with
`loadComponent`, so each feature is downloaded only when the user opens it.
The project uses standalone components, so no `NgModule` is needed for routing.

```ts
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'medications' },
  {
    path: 'medications',
    loadComponent: () =>
      import('./features/medication/medication-list/medication-list')
        .then(m => m.MedicationList),
  },
];
```

### 3. State in services with signals, no NgRx

State lives in services that expose Angular signals. A service keeps a writable
signal private and exposes a read-only version, so only the service can change it.

```ts
@Injectable({ providedIn: 'root' })
export class MedicationService {
  private readonly http = inject(HttpClient);
  private readonly medicationsState = signal<Medication[]>([]);
  readonly medications = this.medicationsState.asReadonly();

  load(): void {
    this.http.get<Medication[]>('/api/medications')
      .subscribe(list => this.medicationsState.set(list));
  }
}
```

## Alternatives considered

- **Organization by type** (`components/`, `services/`, `models/` at the root). It
  would mirror the backend's package-by-layer (ADR 0001), but in Angular one screen
  is a component, a template, styles and a service, and splitting them by type
  spreads one feature across the whole tree. It is also not what the Angular
  documentation recommends.
- **One `NgModule` per feature** (`loadChildren` with modules). This is the older
  approach. Standalone components are the default in recent Angular versions,
  so modules would only add boilerplate.
- **No lazy loading** (every page in the main bundle). It is simpler, but the
  initial download grows with every feature, which hurts on phones.
- **NgRx (Store or SignalStore).** It is powerful and common in large apps, but it
  brings actions, reducers and effects that the MVP does not need. Signals are
  built into Angular and cover the current needs.

## Consequences

- ✅ Each feature lives in one folder: easy to find, easy to delete.
- ✅ Lazy loading keeps the initial bundle small, which matters on phones.
- ✅ No state library to learn or configure; signals are part of Angular itself.
- ⚠️ The frontend is organized by feature while the backend is organized by layer.
  This is deliberate: each side follows the most common convention of its own
  ecosystem.
- ⚠️ The interfaces in `shared/models` are written by hand and must be kept in sync
  with the backend DTOs. If a client is generated from the OpenAPI contract (still
  an open decision), those interfaces will be replaced by the generated ones.
- ⚠️ If state becomes hard to manage (many features sharing state, offline cache),
  NgRx SignalStore will be evaluated in a new ADR that supersedes section 3.
