import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let token: string | null;
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    token = 'test-token';
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { getIdToken: () => Promise.resolve(token) } },
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  async function send(url: string) {
    http.get(url).subscribe();
    await new Promise((resolve) => setTimeout(resolve));
    return backend.expectOne(url).request;
  }

  it('adds the token to requests to our API', async () => {
    const request = await send(`${environment.apiUrl}/api/me`);

    expect(request.headers.get('Authorization')).toBe('Bearer test-token');
  });

  it('does not send the token to other sites', async () => {
    const request = await send('https://example.com/data');

    expect(request.headers.has('Authorization')).toBe(false);
  });

  it('does not send the token to a host that only starts like our API', async () => {
    const request = await send(`${environment.apiUrl}.evil.example/api/me`);

    expect(request.headers.has('Authorization')).toBe(false);
  });

  it('sends the request without a token when nobody is signed in', async () => {
    token = null;

    const request = await send(`${environment.apiUrl}/api/me`);

    expect(request.headers.has('Authorization')).toBe(false);
  });
});
