import { Injectable, computed, signal } from '@angular/core';
import { ItemCarrinho, Jogo } from '../models';

@Injectable({ providedIn: 'root' })
export class CartService {
  itens = signal<ItemCarrinho[]>(this.ler());
  quantidadeTotal = computed(() => this.itens().reduce((s, i) => s + i.quantidade, 0));
  total = computed(() => this.itens().reduce((s, i) => s + i.jogo.preco * i.quantidade, 0));

  adicionar(jogo: Jogo) {
    const lista = this.itens().map(i => ({ ...i }));
    const existente = lista.find(i => i.jogo.id === jogo.id);
    if (existente) existente.quantidade++;
    else lista.push({ jogo, quantidade: 1 });
    this.atualizar(lista);
  }

  alterarQuantidade(id: number | undefined, quantidade: number) {
    if (quantidade < 1) return this.remover(id);
    this.atualizar(this.itens().map(i => i.jogo.id === id ? { ...i, quantidade } : i));
  }

  remover(id: number | undefined) {
    this.atualizar(this.itens().filter(i => i.jogo.id !== id));
  }

  limpar() { this.atualizar([]); }

  private atualizar(lista: ItemCarrinho[]) {
    this.itens.set(lista);
    localStorage.setItem('carrinho', JSON.stringify(lista));
  }

  private ler(): ItemCarrinho[] {
    try { return JSON.parse(localStorage.getItem('carrinho') ?? '[]'); } catch { return []; }
  }
}
