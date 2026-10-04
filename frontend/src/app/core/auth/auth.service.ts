import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { initializeApp } from 'firebase/app';
import {
  GoogleAuthProvider,
  User,
  getAuth,
  onAuthStateChanged,
  signInWithPopup,
  signOut,
} from 'firebase/auth';
import { firstValueFrom } from 'rxjs';

import { environment } from '../../../environments/environment';
import { UserProfile } from '../../shared/models/user-profile';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly auth = getAuth(initializeApp(environment.firebase));

  /** `undefined` while Firebase is still restoring a previous session. */
  readonly user = signal<User | null | undefined>(undefined);
  readonly profile = signal<UserProfile | null>(null);

  constructor() {
    onAuthStateChanged(this.auth, (user) => {
      this.user.set(user);
      this.profile.set(null);
      if (user) {
        void this.loadProfile();
      }
    });
  }

  async signInWithGoogle(): Promise<void> {
    await signInWithPopup(this.auth, new GoogleAuthProvider());
  }

  async signOut(): Promise<void> {
    await signOut(this.auth);
  }

  async isSignedIn(): Promise<boolean> {
    await this.auth.authStateReady();
    return this.auth.currentUser !== null;
  }

  async getIdToken(): Promise<string | null> {
    return (await this.auth.currentUser?.getIdToken()) ?? null;
  }

  private async loadProfile(): Promise<void> {
    try {
      const profile = await firstValueFrom(
        this.http.get<UserProfile>(`${environment.apiUrl}/api/me`),
      );
      this.profile.set(profile);
    } catch {
      this.profile.set(null);
    }
  }
}
