import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CurrencyPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { CartService } from '../core/cart.service';
import { ToastService } from '../core/toast.service';

@Component({
  selector: 'app-carrinho',
  standalone: true,
  imports: [CurrencyPipe, RouterLink],
  template: `
    <h1>Carrinho</h1>
    @if (cart.itens().length === 0) {
      <p class="empty">Seu carrinho está vazio. <a routerLink="/catalogo"><u>Ver jogos</u></a></p>
    } @else {
      <div class="table-wrap">
        <table>
          <thead><tr><th>Jogo</th><th>Preço</th><th>Quantidade</th><th>Subtotal</th><th></th></tr></thead>
          <tbody>
            @for (i of cart.itens(); track i.jogo.id) {
              <tr>
                <td>{{ i.jogo.titulo }}</td>
                <td>{{ i.jogo.preco | currency:'BRL' }}</td>
                <td>
                  <div class="actions">
                    <button class="btn" (click)="cart.alterarQuantidade(i.jogo.id, i.quantidade - 1)" aria-label="Diminuir">−</button>
                    <span class="qtd">{{ i.quantidade }}</span>
                    <button class="btn" (click)="cart.alterarQuantidade(i.jogo.id, i.quantidade + 1)" aria-label="Aumentar">+</button>
                  </div>
                </td>
                <td>{{ i.jogo.preco * i.quantidade | currency:'BRL' }}</td>
                <td><button class="btn btn-danger" (click)="remover(i.jogo.id, i.jogo.titulo)">Remover</button></td>
              </tr>
            }
          </tbody>
        </table>
      </div>
      <div class="row total">
        <h2>Total: {{ cart.total() | currency:'BRL' }}</h2>
        <button class="btn btn-primary" (click)="finalizar()" [disabled]="enviando()">Finalizar compra</button>
      </div>
      <p class="meta">Compra simulada: nenhum pagamento real é feito.</p>
      @if (erro()) { <p class="erro">{{ erro() }}</p> }
    }
  `,
  styles: [`.qtd { min-width: 2rem; text-align: center; align-self: center; } .total { margin-top: 1.25rem; }`]
})
export class CarrinhoComponent {
  private api = inject(ApiService);
  private auth = inject(AuthService);
  private router = inject(Router);
  cart = inject(CartService);
  private toast = inject(ToastService);

  erro = signal('');
  enviando = signal(false);

  /**
   * Chave de idempotência da tentativa de compra em andamento. Enquanto o carrinho for o mesmo, um retry
   * (duplo clique, timeout) reaproveita a chave e o back devolve o pedido já criado em vez de duplicar.
   * Se o carrinho mudar, a chave muda (é outra compra).
   */
  private tentativa: { carrinho: string; chave: string } | null = null;

  remover(id: number | undefined, titulo: string) {
    this.cart.remover(id);
    this.toast.info(`${titulo} removido do carrinho.`);
  }

  finalizar() {
    if (this.enviando()) return;
    if (!this.auth.logado()) { this.toast.aviso('Entre na sua conta para finalizar a compra.'); this.router.navigate(['/login']); return; }
    this.erro.set('');
    this.enviando.set(true);
    this.api.criarPedido(this.cart.itens(), this.chaveDaTentativa()).subscribe({
      next: () => {
        this.tentativa = null;
        this.cart.limpar();
        this.enviando.set(false);
        this.toast.sucesso('Compra realizada com sucesso!');
        this.router.navigate(['/pedidos']);
      },
      error: (e: HttpErrorResponse) => {
        this.enviando.set(false);
        if (e.status === 401) return; // o interceptor já avisou "sessão expirada" e levou para o login
        // 4xx = o back recusou e NÃO criou pedido: a próxima tentativa pode ser uma compra nova.
        // 0 (rede) / 5xx = não sabemos se o pedido foi criado: mantém a chave para o retry ser seguro.
        if (e.status >= 400 && e.status < 500) this.tentativa = null;
        const msg = e.error?.mensagem ?? 'Não foi possível concluir a compra. Tente novamente.';
        this.erro.set(msg);
        this.toast.erro(msg);
      }
    });
  }

  private chaveDaTentativa(): string {
    const carrinho = JSON.stringify(this.cart.itens().map(i => [i.jogo.id, i.quantidade]));
    if (!this.tentativa || this.tentativa.carrinho !== carrinho) {
      this.tentativa = { carrinho, chave: novaChave() };
    }
    return this.tentativa.chave;
  }
}

/** UUID quando disponível (exige contexto seguro: https ou localhost); senão um id aleatório equivalente. */
function novaChave(): string {
  return globalThis.crypto?.randomUUID?.()
    ?? `k${Date.now().toString(36)}${Math.random().toString(36).slice(2, 12)}`;
}
