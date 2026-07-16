import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface AuthResponse {
  token: string;
  email: string;
  displayName: string;
  role: string;
  userId: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'ngs_auth';
  readonly user = signal<AuthResponse | null>(this.read());

  constructor(private http: HttpClient, private router: Router) {}

  register(email: string, password: string, displayName: string) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, {
      email, password, displayName
    }).pipe(tap(res => this.persist(res)));
  }

  login(email: string, password: string) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, {
      email, password
    }).pipe(tap(res => this.persist(res)));
  }

  logout(): void {
    localStorage.removeItem(this.storageKey);
    this.user.set(null);
    this.router.navigateByUrl('/login');
  }

  token(): string | null {
    return this.user()?.token ?? null;
  }

  isLoggedIn(): boolean {
    return !!this.token();
  }

  private persist(res: AuthResponse): void {
    localStorage.setItem(this.storageKey, JSON.stringify(res));
    this.user.set(res);
  }

  private read(): AuthResponse | null {
    const raw = localStorage.getItem(this.storageKey);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthResponse;
    } catch {
      return null;
    }
  }
}
