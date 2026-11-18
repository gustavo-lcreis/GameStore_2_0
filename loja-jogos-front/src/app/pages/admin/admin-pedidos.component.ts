import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { Pedido, VendasResposta } from '../../models';

@Component({
  selector: 'app-admin-pedidos',
  standalone: true,
  imports: [CurrencyPipe, DatePipe],
  template: `
    @if (erro()) { <p class="erro">{{ erro() }}</p> }
    @else if (carregando()) { <p class="empty">Carregando pedidos…</p> }
    @else if (pedidos().length === 0) { <p class="empty">Nenhum pedido realizado.</p> }

    @if (resumo(); as r) {
      <p class="meta">{{ r.resumo.quantidadeVendas }} pedido(s) · {{ r.resumo.valorTotal | currency:'BRL' }} no total</p>
    }
    <div class="table-wrap">
      <table>
        <thead><tr><th>Pedido</th><th>Cliente</th><th>Data</th><th>Itens</th><th>Total</th><th>Status</th></tr></thead>
        <tbody>
          @for (p of pedidos(); track p.id) {
            <tr>
              <td>#{{ p.id }}</td>
              <td>{{ p.clienteNome }}<br><span class="meta">{{ p.clienteEmail }}</span></td>
              <td>{{ p.dataHora | date:'dd/MM/yyyy HH:mm' }}</td>
              <td>@for (i of p.itens; track $index) { <div>{{ i.quantidade }}× {{ i.titulo }}</div> }</td>
              <td>{{ p.total | currency:'BRL' }}</td>
              <td>{{ p.status }}</td>
            </tr>
          }
        </tbody>
      </table>
    </div>
    @if (resumo(); as r) {
      @if (r.totalPages > 1) {
        <div class="row pager">
          <button class="btn" (click)="ir(r.page - 1)" [disabled]="r.page === 0 || carregando()">Anterior</button>
          <span class="meta">Página {{ r.page + 1 }} de {{ r.totalPages }}</span>
          <button class="btn" (click)="ir(r.page + 1)" [disabled]="r.page + 1 >= r.totalPages || carregando()">Próxima</button>
        </div>
      }
    }
  `,
  styles: [`.pager { margin-top: 1rem; justify-content: center; }`]
})
export class AdminPedidosComponent implements OnInit {
  private api = inject(ApiService);
  resumo = signal<VendasResposta | null>(null);
  pedidos = signal<Pedido[]>([]);
  carregando = signal(true);
  erro = signal('');

  ngOnInit() { this.ir(0); }

  ir(pagina: number) {
    this.carregando.set(true);
    this.api.vendas(pagina, 20).subscribe({
      next: r => { this.resumo.set(r); this.pedidos.set(r.vendas); this.carregando.set(false); this.erro.set(''); },
      error: (e: HttpErrorResponse) => {
        this.carregando.set(false);
        this.erro.set(e.error?.mensagem ?? `Erro ao carregar pedidos (HTTP ${e.status}).`);
      }
    });
  }
}
