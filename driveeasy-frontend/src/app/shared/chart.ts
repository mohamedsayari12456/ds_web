import { Component, ElementRef, effect, input, OnDestroy, viewChild } from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

Chart.register(...registerables);

/** Enveloppe minimale de Chart.js : redessine le graphique quand la configuration change. */
@Component({
  selector: 'app-chart',
  template: `<div [style.height.px]="hauteur()"><canvas #canvas></canvas></div>`,
})
export class ChartComponent implements OnDestroy {
  readonly config = input.required<ChartConfiguration>();
  readonly hauteur = input(260);
  private readonly canvas = viewChild.required<ElementRef<HTMLCanvasElement>>('canvas');
  private chart?: Chart;

  constructor() {
    effect(() => {
      const cfg = this.config();
      this.chart?.destroy();
      this.chart = new Chart(this.canvas().nativeElement, {
        ...cfg,
        options: { responsive: true, maintainAspectRatio: false, ...cfg.options },
      });
    });
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }
}
