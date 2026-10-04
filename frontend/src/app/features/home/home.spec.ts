import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { registerFakeIcons } from '../../../testing/fake-icons';
import { AuthService } from '../../core/auth/auth.service';
import { UserProfile } from '../../shared/models/user-profile';
import { Home } from './home';

describe('Home', () => {
  let fixture: ComponentFixture<Home>;
  const profile = signal<UserProfile | null>(null);
  const user = signal<{ displayName: string | null } | null>(null);
  const signOut = vi.fn().mockResolvedValue(undefined);

  beforeEach(async () => {
    profile.set({ id: 1, email: 'maria@example.com', displayName: 'Maria Silva' });
    user.set({ displayName: 'Maria Silva' });
    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { profile, user, signOut } },
      ],
    }).compileComponents();
    registerFakeIcons('logout');

    fixture = TestBed.createComponent(Home);
    await fixture.whenStable();
  });

  function text(selector: string) {
    return (fixture.nativeElement as HTMLElement).querySelector(selector)?.textContent?.trim();
  }

  it('should render the app name as the heading', () => {
    expect(text('h1')).toBe('Apotheca');
  });

  it('greets the user by first name', () => {
    expect(text('p')).toBe('Olá, Maria!');
  });

  it('uses the Google name while the profile is not loaded yet', async () => {
    profile.set(null);
    user.set({ displayName: 'João Souza' });
    await fixture.whenStable();

    expect(text('p')).toBe('Olá, João!');
  });

  it('signs out and goes to the login page', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);

    (fixture.nativeElement as HTMLElement).querySelector('button')!.click();
    await fixture.whenStable();

    expect(signOut).toHaveBeenCalled();
    expect(navigate).toHaveBeenCalledWith('/login');
  });
});
