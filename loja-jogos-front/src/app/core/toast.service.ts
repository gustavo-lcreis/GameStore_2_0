import { Injectable, signal } from '@angular/core';

export type ToastTipo = 'sucesso' | 'erro' | 'aviso' | 'info';
export interface ToastMensagem { id: number; tipo: ToastTipo; texto: string; }

@Injectable({ providedIn: 'root' })
export class ToastService {
  mensagens = signal<ToastMensagem[]>([]);
  private proximoId = 0;
  private temporizadores = new Map<number, ReturnType<typeof setTimeout>>();

  sucesso(texto: string) { this.mostrar(texto, 'sucesso'); }
  erro(texto: string) { this.mostrar(texto, 'erro'); }
  aviso(texto: string) { this.mostrar(texto, 'aviso'); }
  info(texto: string) { this.mostrar(texto, 'info'); }

  fechar(id: number) {
    const timer = this.temporizadores.get(id);
    if (timer) clearTimeout(timer);
    this.temporizadores.delete(id);
    this.mensagens.update(lista => lista.filter(item => item.id !== id));
  }

  private mostrar(texto: string, tipo: ToastTipo) {
    const id = ++this.proximoId;
    this.mensagens.update(lista => [...lista, { id, tipo, texto }]);
    this.temporizadores.set(id, setTimeout(() => this.fechar(id), 4000));
  }
}
