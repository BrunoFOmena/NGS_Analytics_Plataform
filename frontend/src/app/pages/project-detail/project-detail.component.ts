import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';

@Component({
  selector: 'app-project-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section *ngIf="project">
      <header class="page-head">
        <div>
          <h1>{{ project.name }}</h1>
          <p>{{ project.description || 'Project samples' }}</p>
        </div>
      </header>

      <form class="inline-form" (ngSubmit)="createSample()">
        <input [(ngModel)]="sampleName" name="sampleName" placeholder="Sample name" required />
        <input [(ngModel)]="notes" name="notes" placeholder="Notes" />
        <input [(ngModel)]="fastaRef" name="fastaRef" placeholder="FASTA reference name (optional)" />
        <button type="submit">Add sample</button>
      </form>

      <ul class="list">
        <li *ngFor="let s of samples">
          <a [routerLink]="['/samples', s.id]">
            <strong>{{ s.name }}</strong>
            <span>{{ s.notes || 'No notes' }}</span>
          </a>
        </li>
      </ul>
    </section>
  `
})
export class ProjectDetailComponent implements OnInit {
  project: any;
  samples: any[] = [];
  sampleName = '';
  notes = '';
  fastaRef = '';
  private projectId = '';

  constructor(private route: ActivatedRoute, private api: ApiService) {}

  ngOnInit(): void {
    this.projectId = this.route.snapshot.paramMap.get('id') || '';
    this.api.project(this.projectId).subscribe(p => this.project = p);
    this.reloadSamples();
  }

  createSample(): void {
    this.api.createSample(this.projectId, this.sampleName, this.notes, this.fastaRef || undefined)
      .subscribe(() => {
        this.sampleName = '';
        this.notes = '';
        this.fastaRef = '';
        this.reloadSamples();
      });
  }

  private reloadSamples(): void {
    this.api.samples(this.projectId).subscribe(s => this.samples = s);
  }
}
