import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { Medication, MedicationRequest } from '../../shared/models/medication';
import { MedicationService } from './medication.service';

describe('MedicationService', () => {
  const url = `${environment.apiUrl}/api/households/7/medications`;
  const dipirona: Medication = medication(1, 'Dipirona', '500 mg');
  const amoxicilina: Medication = medication(2, 'Amoxicilina', '250 mg/5 ml');
  let service: MedicationService;
  let http: HttpTestingController;

  function medication(id: number, name: string, strength: string | null): Medication {
    return {
      id,
      name,
      activeIngredient: null,
      strength,
      form: 'TABLET',
      unit: 'UNIT',
      shelfLifeAfterOpeningDays: null,
      minimumQuantity: null,
    };
  }

  function request({ id, ...rest }: Medication): MedicationRequest {
    void id;
    return rest;
  }

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(MedicationService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function loaded(medications: Medication[]) {
    const loading = service.load(7);
    http.expectOne({ method: 'GET', url }).flush(medications);
    await loading;
  }

  it('loads the household medications', async () => {
    await loaded([dipirona]);

    expect(service.medications()).toEqual([dipirona]);
  });

  it('gets one medication', async () => {
    const getting = service.get(7, 1);
    http.expectOne({ method: 'GET', url: `${url}/1` }).flush(dipirona);

    expect(await getting).toEqual(dipirona);
  });

  it('creates a medication and keeps the list sorted by name', async () => {
    await loaded([dipirona]);

    const creating = service.create(7, request(amoxicilina));
    const sent = http.expectOne({ method: 'POST', url });
    expect(sent.request.body).toEqual(request(amoxicilina));
    sent.flush(amoxicilina);
    await creating;

    expect(service.medications()).toEqual([amoxicilina, dipirona]);
  });

  it('sorts medications with the same name by strength', async () => {
    const dipirona1g = medication(3, 'Dipirona', '1 g');
    await loaded([dipirona]);

    const creating = service.create(7, request(dipirona1g));
    http.expectOne({ method: 'POST', url }).flush(dipirona1g);
    await creating;

    expect(service.medications()).toEqual([dipirona1g, dipirona]);
  });

  it('updates a medication in the list', async () => {
    await loaded([amoxicilina, dipirona]);
    const renamed = { ...dipirona, name: 'Novalgina' };

    const updating = service.update(7, 1, request(renamed));
    http.expectOne({ method: 'PUT', url: `${url}/1` }).flush(renamed);
    await updating;

    expect(service.medications()).toEqual([amoxicilina, renamed]);
  });

  it('removes a medication from the list', async () => {
    await loaded([amoxicilina, dipirona]);

    const removing = service.remove(7, 2);
    http.expectOne({ method: 'DELETE', url: `${url}/2` }).flush(null);
    await removing;

    expect(service.medications()).toEqual([dipirona]);
  });

  it('does not create a partial list when it was never loaded', async () => {
    const creating = service.create(7, request(dipirona));
    http.expectOne({ method: 'POST', url }).flush(dipirona);
    await creating;

    expect(service.medications()).toBeUndefined();
  });
});
