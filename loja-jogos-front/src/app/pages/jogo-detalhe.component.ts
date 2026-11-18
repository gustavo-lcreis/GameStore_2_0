import { Component, OnInit, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { CartService } from '../core/cart.service';
import { Jogo } from '../models';
import { CapaCssPipe } from '../core/capa-css.pipe';
import { ToastService } from '../core/toast.service';

@Component({
  selector: 'app-jogo-detalhe',
  standalone: true,
  imports: [CurrencyPipe, RouterLink, CapaCssPipe],
  template: `
    <a routerLink="/catalogo">← Voltar para os jogos</a>
    @if (erro()) { <p class="erro">{{ erro() }}</p> }
    @if (jogo(); as j) {
      <div class="detalhe">
        <div class="capa" [style.background-image]="j.imagem ? 'url(' + j.imagem + ')' : null"></div>
        <div class="capa" [style.background-image]="j.imagem | capaCss"></div>
        <div>
          <h1>{{ j.titulo }}</h1>
          <p class="meta">{{ j.plataforma }}{{ j.categoria ? ' · ' + j.categoria.nome : '' }}</p>
          <p>{{ j.descricao }}</p>
          <p><strong class="preco">{{ j.preco | currency:'BRL' }}</strong></p>
          <div class="actions">
            <button class="btn btn-primary" (click)="adicionar(j)">Adicionar ao carrinho</button>
            <button class="btn" (click)="comprarAgora(j)">Comprar agora</button>
          </div>
        </div>
      </div>
    }
  `,
  styles: [`
    .detalhe { display: grid; grid-template-columns: minmax(180px, 300px) 1fr; gap: 2rem; margin-top: 1.5rem; }
    .preco { font-size: 1.6rem; }
    @media (max-width: 640px) { .detalhe { grid-template-columns: 1fr; } }
  `]
})
export class JogoDetalheComponent implements OnInit {
  private api = inject(ApiService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  cart = inject(CartService);
  private toast = inject(ToastService);

  jogo = signal<Jogo | null>(null);
  erro = signal('');

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.buscarJogo(id).subscribe({
      next: j => this.jogo.set(j),
      error: () => this.erro.set('Jogo não encontrado.')
    });
  }

  adicionar(j: Jogo) {
    this.cart.adicionar(j);
    this.toast.sucesso(`${j.titulo} adicionado ao carrinho!`);
  }

  comprarAgora(j: Jogo) {
    this.cart.adicionar(j);
    this.toast.info(`${j.titulo} adicionado. Indo para o carrinho...`);
    this.router.navigate(['/carrinho']);
  }
}
