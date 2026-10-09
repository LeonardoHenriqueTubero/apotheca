import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { StorageLocationService } from './storage-location.service';

describe('StorageLocationService', () => {
  const url = `${environment.apiUrl}/api/households/7/storage-locations`;
  let service: StorageLocationService;
  let backend: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(StorageLocationService);
    backend = TestBed.inject(HttpTestingController);

    const loading = service.load(7);
    backend.expectOne(url).flush([
      { id: 1, name: 'Bolsa' },
      { id: 2, name: 'Gaveta da cozinha' },
    ]);
    await loading;
  });

  afterEach(() => backend.verify());

  function names() {
    return service.locations()?.map((location) => location.name);
  }

  it('loads the household locations', () => {
    expect(names()).toEqual(['Bolsa', 'Gaveta da cozinha']);
  });

  it('adds a created location in alphabetical order', async () => {
    const creating = service.create(7, 'Armário do banheiro');
    const request = backend.expectOne(url);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ name: 'Armário do banheiro' });
    request.flush({ id: 3, name: 'Armário do banheiro' });
    await creating;

    expect(names()).toEqual(['Armário do banheiro', 'Bolsa', 'Gaveta da cozinha']);
  });

  it('replaces a renamed location and keeps the order', async () => {
    const renaming = service.rename(7, 1, 'Mochila');
    const request = backend.expectOne(`${url}/1`);
    expect(request.request.method).toBe('PUT');
    request.flush({ id: 1, name: 'Mochila' });
    await renaming;

    expect(names()).toEqual(['Gaveta da cozinha', 'Mochila']);
  });

  it('removes a deleted location', async () => {
    const removing = service.remove(7, 2);
    const request = backend.expectOne(`${url}/2`);
    expect(request.request.method).toBe('DELETE');
    request.flush(null, { status: 204, statusText: 'No Content' });
    await removing;

    expect(names()).toEqual(['Bolsa']);
  });
});
