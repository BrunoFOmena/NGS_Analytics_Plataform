import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <aside class="nav" *ngIf="auth.isLoggedIn()">
        <div class="brand">NGS Analytics</div>
        <nav>
          <a routerLink="/overview" routerLinkActive="active">Overview</a>
          <a routerLink="/projects" routerLinkActive="active">Projects</a>
          <a routerLink="/compare" routerLinkActive="active">Compare</a>
          <a routerLink="/history" routerLinkActive="active">History</a>
        </nav>
        <div class="user">
          <span>{{ auth.user()?.displayName }}</span>
          <button type="button" (click)="auth.logout()">Sign out</button>
        </div>
      </aside>
      <main>
        <router-outlet />
      </main>
    </div>
  `
})
export class AppComponent {
  constructor(public auth: AuthService) {}
}
