import { Component, input } from '@angular/core';

/** Card de indicador: só apresentação (sem lógica de negócio), reutilizável. */
@Component({
  selector: 'app-kpi-card',
  standalone: true,
  template: `
    <article class="kpi">
      <span class="rotulo">{{ rotulo() }}</span>
      <strong class="valor">{{ valor() }}</strong>
      @if (detalhe()) { <small class="detalhe">{{ detalhe() }}</small> }
    </article>
  `,
  styles: [`
    .kpi { background: var(--card); border: 1px solid var(--line); border-radius: 14px; padding: 1.1rem; display: grid; gap: .5rem; min-width: 0; height: 100%; }
    .rotulo { color: var(--muted); font-size: .85rem; }
    .valor { font-size: clamp(1.3rem, 2.4vw, 1.8rem); overflow-wrap: anywhere; }
    .detalhe { color: var(--muted); font-size: .78rem; }
  `]
})
export class KpiCardComponent {
  rotulo = input.required<string>();
  valor = input.required<string>();
  detalhe = input<string>('');
}
