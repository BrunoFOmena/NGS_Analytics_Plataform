import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section>
      <header class="page-head">
        <div>
          <h1>Overview</h1>
          <p>Workspace summary across projects and analyses.</p>
        </div>
      </header>
      <div class="stat-row">
        <span>Projects {{ projectCount }}</span>
        <span>Samples {{ sampleCount }}</span>
        <span>Analyses {{ analysisCount }}</span>
        <span>Done {{ doneCount }}</span>
      </div>
      <ul class="list">
        <li *ngFor="let a of recent">
          <a routerLink="/history">{{ a.status }} · sample {{ a.sampleId }} · {{ a.engine }}</a>
        </li>
      </ul>
    </section>
  `
})
export class OverviewComponent implements OnInit {
  projectCount = 0;
  sampleCount = 0;
  analysisCount = 0;
  doneCount = 0;
  recent: any[] = [];

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.projects().subscribe(p => this.projectCount = p.length);
    this.api.allSamples().subscribe(s => this.sampleCount = s.length);
    this.api.history().subscribe(h => {
      this.recent = h.slice(0, 8);
      this.analysisCount = h.length;
      this.doneCount = h.filter(x => x.status === 'DONE').length;
    });
  }
}
