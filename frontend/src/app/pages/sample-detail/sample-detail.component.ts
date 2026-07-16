import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { NgxEchartsDirective, provideEchartsCore } from 'ngx-echarts';
import * as echarts from 'echarts/core';
import { BarChart, LineChart, PieChart } from 'echarts/charts';
import { GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { EChartsOption } from 'echarts';
import { ApiService } from '../../core/api.service';

echarts.use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TitleComponent, TooltipComponent, CanvasRenderer]);

@Component({
  selector: 'app-sample-detail',
  standalone: true,
  imports: [CommonModule, NgxEchartsDirective],
  providers: [provideEchartsCore({ echarts })],
  template: `
    <section>
      <header class="page-head">
        <div>
          <h1>Sample</h1>
          <p>Upload FASTQ/VCF/FASTA and inspect metrics.</p>
        </div>
      </header>

      <div class="upload-row">
        <input type="file" (change)="onFile($event)" />
        <button type="button" (click)="upload()" [disabled]="!selected">Upload & analyze</button>
        <span *ngIf="status">{{ status }}</span>
      </div>

      <div class="grid-2" *ngIf="files.length">
        <div>
          <h2>Files</h2>
          <ul class="list compact">
            <li *ngFor="let f of files">{{ f.originalFilename }} · {{ f.fileType }} · {{ f.sizeBytes }} B</li>
          </ul>
        </div>
        <div>
          <h2>Analyses</h2>
          <ul class="list compact">
            <li *ngFor="let a of analyses">
              {{ a.status }} · {{ a.engine }}
              <span *ngIf="a.errorMessage" class="error"> — {{ a.errorMessage }}</span>
            </li>
          </ul>
        </div>
      </div>

      <div class="actions" *ngIf="sampleId">
        <button type="button" (click)="download('csv')">Download CSV</button>
        <button type="button" (click)="download('pdf')">Download PDF</button>
      </div>

      <div class="metrics" *ngIf="fastq">
        <h2>Sequencing (FASTQ)</h2>
        <div class="stat-row">
          <span>Reads {{ fastq.readCount }}</span>
          <span>GC {{ fastq.gcContent | number:'1.1-1' }}%</span>
          <span>Mean Q {{ fastq.meanQuality | number:'1.1-1' }}</span>
          <span>Avg len {{ fastq.avgLength | number:'1.0-0' }}</span>
        </div>
        <div echarts [options]="gcChart" class="chart"></div>
        <div echarts [options]="lengthChart" class="chart"></div>
        <div echarts [options]="qualityChart" class="chart"></div>
      </div>

      <div class="metrics" *ngIf="vcf">
        <h2>Variants (VCF)</h2>
        <div class="stat-row">
          <span>Variants {{ vcf.variantCount }}</span>
          <span>SNPs {{ vcf.snpCount }}</span>
          <span>INDELs {{ vcf.indelCount }}</span>
          <span>Ts/Tv {{ vcf.tsTvRatio | number:'1.2-2' }}</span>
        </div>
        <div echarts [options]="chromChart" class="chart"></div>
        <div echarts [options]="typeChart" class="chart"></div>
        <div echarts [options]="filterChart" class="chart"></div>
      </div>
    </section>
  `
})
export class SampleDetailComponent implements OnInit, OnDestroy {
  sampleId = '';
  selected: File | null = null;
  status = '';
  files: any[] = [];
  analyses: any[] = [];
  fastq: any;
  vcf: any;
  gcChart: EChartsOption = {};
  lengthChart: EChartsOption = {};
  qualityChart: EChartsOption = {};
  chromChart: EChartsOption = {};
  typeChart: EChartsOption = {};
  filterChart: EChartsOption = {};
  private timer?: ReturnType<typeof setInterval>;

  constructor(
    private route: ActivatedRoute,
    private api: ApiService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.sampleId = this.route.snapshot.paramMap.get('id') || '';
    this.refresh();
    this.timer = setInterval(() => this.refresh(), 3000);
  }

  ngOnDestroy(): void {
    if (this.timer) clearInterval(this.timer);
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selected = input.files?.[0] || null;
  }

  upload(): void {
    if (!this.selected) return;
    this.status = 'Uploading...';
    this.api.upload(this.sampleId, this.selected).subscribe({
      next: () => {
        this.status = 'Queued / running analysis';
        this.selected = null;
        this.refresh();
      },
      error: (err) => this.status = err?.error?.message || 'Upload failed'
    });
  }

  private refresh(): void {
    this.api.files(this.sampleId).subscribe(f => this.files = f);
    this.api.analyses(this.sampleId).subscribe(a => this.analyses = a);
    this.api.fastqMetrics(this.sampleId).subscribe({
      next: (m) => {
        this.fastq = m;
        this.gcChart = {
          title: { text: 'Base composition' },
          tooltip: {},
          series: [{
            type: 'pie',
            radius: '60%',
            data: Object.entries(m.baseComposition || {}).map(([name, value]) => ({
              name,
              value: Number(value)
            }))
          }]
        };
        const lengths = m.lengthDistribution || {};
        this.lengthChart = {
          title: { text: 'Read length distribution' },
          xAxis: { type: 'category', data: Object.keys(lengths) },
          yAxis: { type: 'value' },
          series: [{ type: 'bar', data: Object.values(lengths).map(v => Number(v)) }]
        };
        const pq = m.perPositionQuality || {};
        this.qualityChart = {
          title: { text: 'Quality by position' },
          xAxis: { type: 'category', data: Object.keys(pq) },
          yAxis: { type: 'value' },
          series: [{ type: 'line', data: Object.values(pq).map(v => Number(v)), areaStyle: {} }]
        };
      },
      error: () => {}
    });
    this.api.vcfMetrics(this.sampleId).subscribe({
      next: (m) => {
        this.vcf = m;
        const chrom = m.chromosomeDistribution || {};
        this.chromChart = {
          title: { text: 'Variants by chromosome' },
          xAxis: { type: 'category', data: Object.keys(chrom) },
          yAxis: { type: 'value' },
          series: [{ type: 'bar', data: Object.values(chrom).map(v => Number(v)) }]
        };
        this.typeChart = {
          title: { text: 'Variant types' },
          tooltip: {},
          series: [{
            type: 'pie',
            radius: ['35%', '65%'],
            data: [
              { name: 'SNP', value: Number(m.snpCount) },
              { name: 'INDEL', value: Number(m.indelCount) },
              { name: 'MNP', value: Number(m.mnpCount) }
            ]
          }]
        };
        const filters = m.filterDistribution || {};
        this.filterChart = {
          title: { text: 'Filter distribution' },
          xAxis: { type: 'category', data: Object.keys(filters) },
          yAxis: { type: 'value' },
          series: [{ type: 'bar', data: Object.values(filters).map(v => Number(v)) }]
        };
      },
      error: () => {}
    });
  }

  download(kind: 'csv' | 'pdf'): void {
    const url = kind === 'csv'
      ? this.api.reportCsvUrl(this.sampleId)
      : this.api.reportPdfUrl(this.sampleId);
    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = `report-${this.sampleId}.${kind}`;
        a.click();
        URL.revokeObjectURL(a.href);
      },
      error: () => this.status = 'Report not available yet'
    });
  }
}
