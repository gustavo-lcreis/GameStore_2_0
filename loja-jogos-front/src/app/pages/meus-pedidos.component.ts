import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../core/api.service';
import { ToastService } from '../core/toast.service';
import { Pedido } from '../models';

@Component({
  selector: 'app-meus-pedidos',
  standalone: true,
  imports: [CurrencyPipe, DatePipe],
  template: `
    <h1>Meus pedidos</h1>
    @if (carregando()) {
      <p class="empty">Carregando seus pedidos…</p>
    } @else if (erro()) {
      <p class="erro">{{ erro() }}</p>
      <button class="btn" (click)="carregar()">Tentar novamente</button>
    } @else if (pedidos().length === 0) {
      <p class="empty">Você ainda não fez nenhum pedido.</p>
    }
    @for (p of pedidos(); track p.id) {
      <article class="card pedido">
        <div class="row">
          <strong>Pedido #{{ p.id }}</strong>
          <span class="meta">{{ p.dataHora | date:'dd/MM/yyyy HH:mm' }} · {{ p.status }}</span>
        </div>
        <ul>
          @for (i of p.itens; track $index) {
            <li>{{ i.quantidade }}× {{ i.titulo }} — {{ i.precoUnitario | currency:'BRL' }}</li>
          }
        </ul>
        <strong>Total: {{ p.total | currency:'BRL' }}</strong>
      </article>
    }
  `,
  styles: [`.pedido { margin-bottom: 1rem; } ul { margin: .25rem 0 .5rem; padding-left: 1.2rem; }`]
})
export class MeusPedidosComponent implements OnInit {
  private api = inject(ApiService);
  private toast = inject(ToastService);

  pedidos = signal<Pedido[]>([]);
  carregando = signal(true);
  erro = signal('');

  ngOnInit() { this.carregar(); }

  /** Erro NÃO pode virar lista vazia: antes um 404/401 aparecia como "você ainda não fez nenhum pedido". */
  carregar() {
    this.carregando.set(true);
    this.erro.set('');
    this.api.meusPedidos().subscribe({
      next: p => { this.pedidos.set(p); this.carregando.set(false); },
      error: (e: HttpErrorResponse) => {
        this.carregando.set(false);
        if (e.status === 401) return; // interceptor já levou para o login
        const msg = e.error?.mensagem ?? `Não foi possível carregar seus pedidos (HTTP ${e.status}).`;
        this.erro.set(msg);
        this.toast.erro(msg);
      }
    });
  }
}
