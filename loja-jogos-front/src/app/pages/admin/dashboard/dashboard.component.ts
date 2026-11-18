import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { ApiService } from '../../../core/api.service';
import { Agrupamento, DashboardResposta } from '../../../models';
import { AdminUsuariosComponent } from '../admin-usuarios.component';
import { GraficoEvolucaoComponent } from './grafico-evolucao.component';
import { KpiCardComponent } from './kpi-card.component';

interface Atalho { rotulo: string; dias: number; agrupamento: Agrupamento; }

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CurrencyPipe, DecimalPipe, RouterLink, KpiCardComponent, GraficoEvolucaoComponent, AdminUsuariosComponent],
  template: `
    <section class="cab">
      <div><h2>Visão geral</h2><p>Indicadores de vendas do site no período escolhido.</p></div>
      <a class="link" routerLink="/admin/pedidos">Ver todos os pedidos →</a>
    </section>

    <form class="filtros" (submit)="aplicar($event)">
      <div class="atalhos" role="group" aria-label="Períodos rápidos">
        @for (a of atalhos; track a.rotulo) {
          <button type="button" class="btn" (click)="usarAtalho(a)">{{ a.rotulo }}</button>
        }
      </div>
      <label>De <input type="date" [value]="inicio()" (change)="inicio.set($any($event.target).value)"></label>
      <label>Até <input type="date" [value]="fim()" (change)="fim.set($any($event.target).value)"></label>
      <label>Agrupar por
        <select [value]="agrupamento()" (change)="agrupamento.set($any($event.target).value)">
          <option value="DIA">Dia</option>
          <option value="MES">Mês</option>
        </select>
      </label>
      <button type="submit" class="btn btn-primary" [disabled]="carregando()">Aplicar</button>
    </form>

    @if (erro()) {
      <p class="erro">{{ erro() }}</p>
      <button class="btn" (click)="carregar()">Tentar novamente</button>
    }

    @if (dados(); as d) {
      <div [class.carregando]="carregando()" [attr.aria-busy]="carregando()">
        <section class="kpis">
          <app-kpi-card rotulo="Pedidos no período" [valor]="(d.kpis.totalPedidos | number:'1.0-0') ?? ''"
                        [detalhe]="(d.kpis.totalPedidosGeral | number:'1.0-0') + ' no total da loja'" />
          <app-kpi-card rotulo="Faturamento" [valor]="(d.kpis.faturamento | currency:'BRL') ?? ''" detalhe="Pedidos concluídos" />
          <app-kpi-card rotulo="Ticket médio" [valor]="(d.kpis.ticketMedio | currency:'BRL') ?? ''" detalhe="Faturamento ÷ pedidos" />
          <app-kpi-card rotulo="Jogos vendidos" [valor]="(d.kpis.jogosVendidos | number:'1.0-0') ?? ''" detalhe="Soma das quantidades" />
        </section>

        <section class="painel">
          <h3>Evolução de vendas</h3>
          <p class="meta">Faturamento por {{ d.agrupamento === 'MES' ? 'mês' : 'dia' }}</p>
          <app-grafico-evolucao [pontos]="d.evolucao" [agrupamento]="d.agrupamento" />
        </section>

        <div class="duas">
          <section class="painel">
            <h3>Jogos mais vendidos</h3>
            @if (d.topJogos.length === 0) { <p class="empty">Nenhuma venda no período.</p> }
            @else {
              <div class="table-wrap"><table>
                <thead><tr><th>#</th><th>Jogo</th><th>Qtd.</th><th>Receita</th></tr></thead>
                <tbody>
                  @for (j of d.topJogos; track j.jogoId; let i = $index) {
                    <tr><td>{{ i + 1 }}</td><td>{{ j.titulo }}</td><td>{{ j.quantidade }}</td><td>{{ j.receita | currency:'BRL' }}</td></tr>
                  }
                </tbody>
              </table></div>
            }
          </section>

          <section class="painel">
            <h3>Pedidos por status</h3>
            <div class="table-wrap"><table>
              <thead><tr><th>Status</th><th>Pedidos</th><th>Valor</th></tr></thead>
              <tbody>
                @for (s of d.porStatus; track s.status) {
                  <tr><td>{{ s.status }}</td><td>{{ s.pedidos }}</td><td>{{ s.faturamento | currency:'BRL' }}</td></tr>
                }
              </tbody>
            </table></div>
          </section>
        </div>
      </div>
    } @else if (carregando()) {
      <p class="empty">Carregando indicadores…</p>
    }

    <section class="painel" id="contas">
      <h3>Contas do site</h3>
      <p class="meta">Busque e exclua contas de clientes. Contas de administrador não podem ser excluídas.</p>
      <app-admin-usuarios />
    </section>
  `,
  styles: [`
    :host { display: block; color: var(--ink); }
    .cab { display: flex; justify-content: space-between; align-items: center; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; }
    .cab h2 { margin: 0 0 .25rem; font-size: 1.45rem; } .cab p { margin: 0; color: var(--muted); }
    .link { color: var(--accent); font-weight: 700; font-size: .9rem; }
    .filtros { display: flex; flex-wrap: wrap; gap: .75rem; align-items: end; margin-bottom: 1.25rem; }
    .filtros label { margin: 0; font-size: .85rem; } .filtros input, .filtros select { width: auto; }
    .atalhos { display: flex; gap: .5rem; flex-wrap: wrap; }
    .kpis { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1rem; margin-bottom: 1.25rem; }
    .painel { background: var(--card); border: 1px solid var(--line); border-radius: 14px; padding: 1.25rem; margin-bottom: 1.25rem; min-width: 0; }
    .painel h3 { margin: 0 0 .25rem; } .painel .meta { margin-bottom: .75rem; }
    .duas { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
    .carregando { opacity: .55; transition: opacity .15s; }
    @media (max-width: 900px) { .kpis { grid-template-columns: repeat(2, minmax(0, 1fr)); } .duas { grid-template-columns: 1fr; } }
  `]
})
export class AdminDashboardComponent implements OnInit, OnDestroy {
  private api = inject(ApiService);
  private requisicao?: Subscription;

  readonly atalhos: Atalho[] = [
    { rotulo: '7 dias', dias: 7, agrupamento: 'DIA' },
    { rotulo: '30 dias', dias: 30, agrupamento: 'DIA' },
    { rotulo: '90 dias', dias: 90, agrupamento: 'DIA' },
    { rotulo: '12 meses', dias: 365, agrupamento: 'MES' }
  ];

  inicio = signal(formatar(diasAtras(29)));
  fim = signal(formatar(new Date()));
  agrupamento = signal<Agrupamento>('DIA');

  dados = signal<DashboardResposta | null>(null);
  carregando = signal(false);
  erro = signal('');

  periodoValido = computed(() => !!this.inicio() && !!this.fim() && this.inicio() <= this.fim());

  ngOnInit() { this.carregar(); }
  ngOnDestroy() { this.requisicao?.unsubscribe(); }

  usarAtalho(a: Atalho) {
    this.inicio.set(formatar(diasAtras(a.dias - 1)));
    this.fim.set(formatar(new Date()));
    this.agrupamento.set(a.agrupamento);
    this.carregar();
  }

  aplicar(evento: Event) {
    evento.preventDefault();
    this.carregar();
  }

  carregar() {
    if (!this.periodoValido()) { this.erro.set('Informe um período válido: a data final não pode ser anterior à inicial.'); return; }
    this.requisicao?.unsubscribe(); // se o admin clicar rápido, só vale a última consulta
    this.carregando.set(true);
    this.erro.set('');
    this.requisicao = this.api.dashboard(this.inicio(), this.fim(), this.agrupamento()).subscribe({
      next: d => { this.dados.set(d); this.carregando.set(false); },
      error: (e: HttpErrorResponse) => {
        this.carregando.set(false);
        if (e.status === 401) return; // interceptor já levou para o login
        this.erro.set(e.error?.mensagem ?? `Não foi possível carregar o dashboard (HTTP ${e.status}).`);
      }
    });
  }
}

function diasAtras(n: number): Date {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d;
}

/** yyyy-MM-dd no fuso LOCAL (toISOString() usaria UTC e, à noite no Brasil, mandaria o dia seguinte). */
function formatar(d: Date): string {
  const dois = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${dois(d.getMonth() + 1)}-${dois(d.getDate())}`;
}
