import { Injectable, signal } from '@angular/core';

const TOKEN_KEY = 'ezpay.token';
const USER_ID_KEY = 'ezpay.userId';
const USERNAME_KEY = 'ezpay.username';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly token = signal<string | null>(localStorage.getItem(TOKEN_KEY));
  readonly username = signal<string | null>(localStorage.getItem(USERNAME_KEY));
  readonly userId = signal<string | null>(localStorage.getItem(USER_ID_KEY));

  get isLoggedIn(): boolean {
    return !!this.token();
  }

  setSession(accessToken: string, userId: string, username: string): void {
    localStorage.setItem(TOKEN_KEY, accessToken);
    localStorage.setItem(USER_ID_KEY, userId);
    localStorage.setItem(USERNAME_KEY, username);
    this.token.set(accessToken);
    this.userId.set(userId);
    this.username.set(username);
  }

  clearSession(): void {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_ID_KEY);
    localStorage.removeItem(USERNAME_KEY);
    this.token.set(null);
    this.userId.set(null);
    this.username.set(null);
  }
}
