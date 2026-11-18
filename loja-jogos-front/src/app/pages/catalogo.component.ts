// loja-jogos-front/src/app/pages/catalogo.component.ts
import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { CartService } from '../core/cart.service';
import { ToastService } from '../core/toast.service';
import { CapaCssPipe } from '../core/capa-css.pipe';
import { Categoria, Jogo } from '../models';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [FormsModule, RouterLink, CurrencyPipe, CapaCssPipe],
  template: `
    <h1>Catálogo de jogos</h1>
    <div class="toolbar">
      <input type="search" placeholder="Buscar jogo pelo título" aria-label="Buscar jogo pelo título"
             [(ngModel)]="busca" (ngModelChange)="carregar()">
      <select [(ngModel)]="categoria" (ngModelChange)="carregar()" aria-label="Filtrar por categoria">
        <option [ngValue]="null">Todas as categorias</option>
        @for (c of categorias(); track c.id) { <option [ngValue]="c.id">{{ c.nome }}</option> }
      </select>
    </div>
    @if (erro()) { <p class="erro">{{ erro() }}</p> }
    @if (!erro() && jogos().length === 0) {
      <p class="empty">Nenhum jogo encontrado. Tente outro título ou outra categoria.</p>
    }
    <div class="grid">
      @for (j of jogos(); track j.id) {
        <article class="card">
          <a [routerLink]="['/jogo', j.id]">
            <div class="capa" [style.background-image]="j.imagem | capaCss"></div>
            <h3>{{ j.titulo }}</h3>
          </a>
          <p class="meta">{{ j.plataforma }}{{ j.categoria ? ' · ' + j.categoria.nome : '' }}</p>
          <div class="row">
            <strong>{{ j.preco | currency:'BRL' }}</strong>
            <button class="btn btn-primary" (click)="adicionar(j)">Adicionar</button>
          </div>
        </article>
      }
    </div>
  `
})
export class CatalogoComponent implements OnInit {
  private api = inject(ApiService);
  cart = inject(CartService);
  private toast = inject(ToastService);

  adicionar(j: Jogo) {
    this.cart.adicionar(j);
    this.toast.sucesso(`${j.titulo} adicionado ao carrinho!`);
  }

  jogos = signal<Jogo[]>([]);
  categorias = signal<Categoria[]>([]);
  erro = signal('');
  busca = '';
  categoria: number | null = null;

  ngOnInit() {
    this.api.listarCategorias().subscribe({ next: c => this.categorias.set(c), error: () => {} });
    this.carregar();
  }

  carregar() {
    this.api.listarJogos(this.busca.trim(), this.categoria).subscribe({
      next: j => { this.jogos.set(j); this.erro.set(''); },
      error: () => this.erro.set('Não foi possível carregar os jogos. Verifique se a API está rodando.')
    });
  }
}