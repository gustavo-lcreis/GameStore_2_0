import { Component, computed, input } from '@angular/core';
import { Agrupamento, PontoEvolucao } from '../../../models';

/**
 * Gráfico de barras em SVG puro (sem biblioteca): faturamento por dia/mês.
 * Quando o volume ou a interatividade exigirem mais, dá para trocar por Chart.js/ngx-charts sem mexer na tela.
 */
@Component({
  selector: 'app-grafico-evolucao',
  standalone: true,
  template: `
    <svg class="grafico" [attr.viewBox]="'0 0 ' + largura + ' ' + (altura + 30)" role="img" [attr.aria-label]="descricao()">
      @for (g of grades(); track g.y) {
        <line [attr.x1]="margemEsq" [attr.x2]="largura" [attr.y1]="g.y" [attr.y2]="g.y" class="grade" />
        <text [attr.x]="margemEsq - 6" [attr.y]="g.y + 4" text-anchor="end" class="eixo">{{ g.rotulo }}</text>
      }
      @for (b of barras(); track b.periodo) {
        <rect [attr.x]="b.x" [attr.y]="b.y" [attr.width]="b.w" [attr.height]="b.h" rx="2" class="barra">
          <title>{{ b.dica }}</title>
        </rect>
      }
      @for (r of rotulos(); track r.x) {
        <text [attr.x]="r.x" [attr.y]="altura + 20" text-anchor="middle" class="eixo">{{ r.texto }}</text>
      }
    </svg>
  `,
  styles: [`
    .grafico { width: 100%; height: auto; display: block; }
    .grade { stroke: var(--line); stroke-width: 1; }
    .eixo { fill: var(--muted); font-size: 11px; }
    .barra { fill: var(--accent); }
    .barra:hover { fill: #3f27c9; }
  `]
})
export class GraficoEvolucaoComponent {
  pontos = input.required<PontoEvolucao[]>();
  agrupamento = input<Agrupamento>('DIA');

  readonly largura = 720;
  readonly altura = 220;
  readonly margemEsq = 64;

  private moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
  private moedaCompacta = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', notation: 'compact', maximumFractionDigits: 1 });

  private maximo = computed(() => Math.max(0, ...this.pontos().map(p => p.faturamento)));

  barras = computed(() => {
    const pts = this.pontos();
    if (pts.length === 0) return [];
    const max = this.maximo() || 1;
    const slot = (this.largura - this.margemEsq) / pts.length;
    const w = Math.max(slot * 0.72, 1);
    return pts.map((p, i) => {
      const h = p.faturamento > 0 ? Math.max((p.faturamento / max) * this.altura, 1) : 0;
      return {
        periodo: p.periodo,
        x: this.margemEsq + i * slot + (slot - w) / 2,
        y: this.altura - h, w, h,
        dica: `${this.rotulo(p.periodo)}: ${this.moeda.format(p.faturamento)} · ${p.pedidos} pedido(s)`
      };
    });
  });

  grades = computed(() => {
    const max = this.maximo();
    return [0, 0.25, 0.5, 0.75, 1].map(f => ({ y: this.altura - f * this.altura, rotulo: this.moedaCompacta.format(max * f) }));
  });

  /** No máximo ~6 rótulos no eixo X (primeiro e último sempre), para não sobrepor texto. */
  rotulos = computed(() => {
    const pts = this.pontos();
    if (pts.length === 0) return [];
    const slot = (this.largura - this.margemEsq) / pts.length;
    const passo = Math.max(1, Math.ceil(pts.length / 6));
    const indices = new Set<number>();
    for (let i = 0; i < pts.length; i += passo) indices.add(i);
    indices.add(pts.length - 1);
    // se o último ficou colado no anterior, descarta o anterior
    const ordenados = [...indices].sort((a, b) => a - b);
    const finais = ordenados.filter((idx, k) => !(k < ordenados.length - 1 && ordenados[ordenados.length - 1] - idx < passo / 2 && idx !== 0));
    return finais.map(i => ({ x: this.margemEsq + i * slot + slot / 2, texto: this.rotulo(pts[i].periodo) }));
  });

  descricao = computed(() =>
    `Gráfico de barras do faturamento por ${this.agrupamento() === 'MES' ? 'mês' : 'dia'}, ${this.pontos().length} períodos.`);

  /** "2026-11-05" -> "05/11" ; "2026-11" -> "11/2026". Sem new Date(): evita deslocar o dia por fuso horário. */
  private rotulo(periodo: string): string {
    const [ano, mes, dia] = periodo.split('-');
    return dia ? `${dia}/${mes}` : `${mes}/${ano}`;
  }
}
