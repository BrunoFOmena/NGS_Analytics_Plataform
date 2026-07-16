import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgxEchartsDirective, provideEchartsCore } from 'ngx-echarts';
import * as echarts from 'echarts/core';
import { BarChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { EChartsOption } from 'echarts';
import { ApiService } from '../../core/api.service';

echarts.use([BarChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer]);

@Component({
  selector: 'app-compare',
  standalone: true,
  imports: [CommonModule, FormsModule, NgxEchartsDirective],
  providers: [provideEchartsCore({ echarts })],
  template: `
    <section>
      <header class="page-head">
        <div>
          <h1>Compare samples</h1>
          <p>Side-by-side FASTQ and VCF headline metrics.</p>
        </div>
      </header>

      <form class="inline-form" (ngSubmit)="run()">
        <select [(ngModel)]="a" name="a" required>
          <option value="">Sample A</option>
          <option *ngFor="let s of samples" [value]="s.id">{{ s.name }}</option>
        </select>
        <select [(ngModel)]="b" name="b" required>
          <option value="">Sample B</option>
          <option *ngFor="let s of samples" [value]="s.id">{{ s.name }}</option>
        </select>
        <button type="submit">Compare</button>
      </form>

      <p class="error" *ngIf="error">{{ error }}</p>
      <div echarts *ngIf="chart" [options]="chart" class="chart tall"></div>
      <pre *ngIf="result">{{ result | json }}</pre>
    </section>
  `
})
export class CompareComponent implements OnInit {
  samples: any[] = [];
  a = '';
  b = '';
  result: any;
  error = '';
  chart: EChartsOption | null = null;

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.api.allSamples().subscribe(s => this.samples = s);
  }

  run(): void {
    this.error = '';
    this.api.compare(this.a, this.b).subscribe({
      next: (res) => {
        this.result = res;
        const categories: string[] = [];
        const valuesA: number[] = [];
        const valuesB: number[] = [];
        if (res.fastq) {
          for (const [k, v] of Object.entries(res.fastq)) {
            categories.push(`FASTQ ${k}`);
            valuesA.push((v as any).a);
            valuesB.push((v as any).b);
          }
        }
        if (res.vcf) {
          for (const [k, v] of Object.entries(res.vcf)) {
            categories.push(`VCF ${k}`);
            valuesA.push((v as any).a);
            valuesB.push((v as any).b);
          }
        }
        this.chart = {
          tooltip: { trigger: 'axis' },
          legend: { data: [res.sampleA.name, res.sampleB.name] },
          xAxis: { type: 'category', data: categories, axisLabel: { rotate: 30 } },
          yAxis: { type: 'value' },
          series: [
            { name: res.sampleA.name, type: 'bar', data: valuesA },
            { name: res.sampleB.name, type: 'bar', data: valuesB }
          ]
        };
      },
      error: (err) => this.error = err?.error?.message || 'Compare failed'
    });
  }
}
