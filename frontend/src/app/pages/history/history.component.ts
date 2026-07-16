import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section>
      <header class="page-head">
        <div>
          <h1>Analysis history</h1>
          <p>Queued, running, completed, and failed jobs.</p>
        </div>
      </header>
      <table class="table">
        <thead>
          <tr>
            <th>Status</th>
            <th>Engine</th>
            <th>Sample</th>
            <th>Created</th>
            <th>Finished</th>
            <th>Error</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let a of rows">
            <td>{{ a.status }}</td>
            <td>{{ a.engine }}</td>
            <td>{{ a.sampleId }}</td>
            <td>{{ a.createdAt | date:'short' }}</td>
            <td>{{ a.finishedAt ? (a.finishedAt | date:'short') : '—' }}</td>
            <td>{{ a.errorMessage || '—' }}</td>
          </tr>
        </tbody>
      </table>
    </section>
  `
})
export class HistoryComponent implements OnInit {
  rows: any[] = [];

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.history().subscribe(h => this.rows = h);
  }
}
