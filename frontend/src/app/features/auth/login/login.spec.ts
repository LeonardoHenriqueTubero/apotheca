import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { registerFakeIcons } from '../../../../testing/fake-icons';
import { AuthService } from '../../../core/auth/auth.service';
import { Login } from './login';

describe('Login', () => {
  let fixture: ComponentFixture<Login>;
  let signInWithGoogle: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    signInWithGoogle = vi.fn().mockResolvedValue(undefined);
    await TestBed.configureTestingModule({
      imports: [Login],
      providers: [provideRouter([]), { provide: AuthService, useValue: { signInWithGoogle } }],
    }).compileComponents();
    registerFakeIcons('google');

    fixture = TestBed.createComponent(Login);
    await fixture.whenStable();
  });

  async function clickSignIn() {
    (fixture.nativeElement as HTMLElement).querySelector('button')!.click();
    await fixture.whenStable();
  }

  function errorText() {
    return (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]')?.textContent;
  }

  it('signs in with Google and goes to the home page', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    await clickSignIn();

    expect(signInWithGoogle).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith('/');
    expect(errorText()).toBeUndefined();
  });

  it('explains that the login was cancelled when the popup is closed', async () => {
    signInWithGoogle.mockRejectedValue({ code: 'auth/popup-closed-by-user' });

    await clickSignIn();

    expect(errorText()).toContain('O login foi cancelado.');
  });

  it('shows a generic message for unexpected errors', async () => {
    signInWithGoogle.mockRejectedValue(new Error('boom'));

    await clickSignIn();

    expect(errorText()).toContain('Não foi possível entrar. Tente de novo.');
  });
});
