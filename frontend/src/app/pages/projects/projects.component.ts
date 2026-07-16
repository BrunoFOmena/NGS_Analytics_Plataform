import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-projects',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section>
      <header class="page-head">
        <div>
          <h1>Projects</h1>
          <p>Organize samples and sequencing uploads.</p>
        </div>
      </header>

      <form class="inline-form" (ngSubmit)="create()">
        <input [(ngModel)]="name" name="name" placeholder="Project name" required />
        <input [(ngModel)]="description" name="description" placeholder="Description" />
        <button type="submit">Create</button>
      </form>

      <ul class="list">
        <li *ngFor="let p of projects">
          <a [routerLink]="['/projects', p.id]">
            <strong>{{ p.name }}</strong>
            <span>{{ p.description || 'No description' }}</span>
          </a>
        </li>
      </ul>
    </section>
  `
})
export class ProjectsComponent implements OnInit {
  projects: any[] = [];
  name = '';
  description = '';

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  create(): void {
    this.api.createProject(this.name, this.description).subscribe(() => {
      this.name = '';
      this.description = '';
      this.reload();
    });
  }

  private reload(): void {
    this.api.projects().subscribe(p => this.projects = p);
  }
}
