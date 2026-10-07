import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { Household } from '../../shared/models/household';
import { HouseholdService } from './household.service';

describe('HouseholdService', () => {
  const url = `${environment.apiUrl}/api/households`;
  const home: Household = { id: 1, name: 'Casa da Maria', role: 'OWNER' };
  const beach: Household = { id: 2, name: 'Casa da praia', role: 'MEMBER' };
  let service: HouseholdService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(HouseholdService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('starts with nothing loaded', () => {
    expect(service.households()).toBeUndefined();
    expect(service.current()).toBeNull();
  });

  it('loads the households and uses the first as the current one', async () => {
    const loading = service.load();
    http.expectOne({ method: 'GET', url }).flush([home, beach]);

    expect(await loading).toEqual([home, beach]);
    expect(service.current()).toEqual(home);
  });

  it('creates a household and adds it to the list', async () => {
    const creating = service.create('Casa da Maria');
    const request = http.expectOne({ method: 'POST', url });
    expect(request.request.body).toEqual({ name: 'Casa da Maria' });
    request.flush(home);

    expect(await creating).toEqual(home);
    expect(service.households()).toEqual([home]);
    expect(service.current()).toEqual(home);
  });
});
