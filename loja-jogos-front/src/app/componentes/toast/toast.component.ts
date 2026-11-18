import { Component, inject } from '@angular/core';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  template: `
    <section class="toast-area" aria-label="Notificações" aria-live="polite" aria-relevant="additions">
      @for (item of toast.mensagens(); track item.id) {
        <article class="toast" [class.toast--sucesso]="item.tipo === 'sucesso'"
                 [class.toast--erro]="item.tipo === 'erro'"
                 [class.toast--aviso]="item.tipo === 'aviso'"
                 [class.toast--info]="item.tipo === 'info'" role="status">
          <span class="toast__icone" aria-hidden="true">
            {{ item.tipo === 'sucesso' ? '✓' : item.tipo === 'erro' ? '✕' : item.tipo === 'aviso' ? '!' : 'i' }}
          </span>
          <span class="toast__texto">{{ item.texto }}</span>
          <button type="button" class="toast__fechar" (click)="toast.fechar(item.id)" aria-label="Fechar aviso">×</button>
        </article>
      }
    </section>
  `,
  styles: [`
    .toast-area { position: fixed; z-index: 2000; top: 1rem; right: 1rem; display: grid; gap: .65rem; width: min(390px, calc(100vw - 2rem)); pointer-events: none; }
    .toast { display: flex; align-items: center; gap: .7rem; padding: .9rem 1rem; border: 1px solid var(--line); border-left: 5px solid #64748b; border-radius: 10px; background: #fff; color: #1c1a2e; box-shadow: 0 8px 28px #17132b24; animation: toast-in .2s ease-out; pointer-events: auto; }
    .toast--sucesso { border-left-color: #16834a; } .toast--erro { border-left-color: #d8344f; }
    .toast--aviso { border-left-color: #d18a13; } .toast--info { border-left-color: #3978d4; }
    .toast__icone { display: grid; place-items: center; flex: 0 0 1.5rem; width: 1.5rem; height: 1.5rem; border-radius: 50%; background: #edf0f4; font-weight: 700; }
    .toast--sucesso .toast__icone { color: #16834a; background: #e6f5ec; }
    .toast--erro .toast__icone { color: #d8344f; background: #fff0f2; }
    .toast--aviso .toast__icone { color: #a66a00; background: #fff6df; }
    .toast--info .toast__icone { color: #3978d4; background: #edf4ff; }
    .toast__texto { flex: 1; font-size: .92rem; } .toast__fechar { border: 0; background: transparent; color: #6b6880; font-size: 1.35rem; cursor: pointer; }
    @keyframes toast-in { from { opacity: 0; transform: translateX(12px); } to { opacity: 1; transform: translateX(0); } }
    @media (prefers-reduced-motion: reduce) { .toast { animation: none; } }
  `]
})
export class ToastComponent { toast = inject(ToastService); }
