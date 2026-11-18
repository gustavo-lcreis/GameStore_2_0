// loja-jogos-front/src/app/pages/admin/admin-jogos.component.ts
import { Component, ElementRef, OnInit, ViewChild, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CurrencyPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { ToastService } from '../../core/toast.service';
import { aoFalharImagem, urlImagem, validarImagemJpeg } from '../../core/imagem';
import { Categoria, Jogo, JogoFormDados } from '../../models';

const VAZIO: Jogo = { titulo: '', descricao: '', preco: 0, imagem: '', plataforma: '', categoriaId: null };

@Component({
  selector: 'app-admin-jogos',
  standalone: true,
  imports: [FormsModule, CurrencyPipe],
  template: `
    <section class="card form">
      <h2>{{ form.id ? 'Editar jogo' : 'Cadastrar jogo' }}</h2>
      <div class="cols">
        <label>Título <input name="titulo" [(ngModel)]="form.titulo"></label>
        <label>Plataforma <input name="plataforma" [(ngModel)]="form.plataforma" placeholder="PC, PS5, Xbox..."></label>
        <label>Preço (R$) <input type="number" min="0" step="0.01" name="preco" [(ngModel)]="form.preco"></label>
        <label>Categoria
          <select name="categoria" [(ngModel)]="form.categoriaId">
            <option [ngValue]="null">Selecione</option>
            @for (c of categorias(); track c.id) { <option [ngValue]="c.id">{{ c.nome }}</option> }
          </select>
        </label>
      </div>

      <label>Capa do jogo (opcional · JPG/JPEG · máx. 5 MB)
        <input #arquivoInput type="file" name="imagem"
               accept=".jpg,.jpeg,image/jpeg"
               (change)="aoEscolherArquivo($event)">
      </label>
      @if (erroImagem()) { <p class="erro" role="alert">{{ erroImagem() }}</p> }
      @if (preview() ?? urlImagem(form.imagem); as src) {
        <img class="previa" [src]="src" alt="Pré-visualização da capa" (error)="aoFalharImagem($event)">
      }

      <label>Descrição <textarea rows="3" name="descricao" [(ngModel)]="form.descricao"></textarea></label>
      @if (erro()) { <p class="erro" role="alert">{{ erro() }}</p> }
      @if (ok()) { <p class="ok">{{ ok() }}</p> }
      <div class="actions">
        <button class="btn btn-primary" (click)="salvar()" [disabled]="salvando()">
          {{ salvando() ? 'Salvando...' : 'Salvar jogo' }}
        </button>
        @if (form.id) { <button class="btn" (click)="cancelar()">Cancelar edição</button> }
      </div>
    </section>

    <div class="table-wrap">
      <table>
        <thead><tr><th>Título</th><th>Plataforma</th><th>Categoria</th><th>Preço</th><th></th></tr></thead>
        <tbody>
          @for (j of jogos(); track j.id) {
            <tr>
              <td>{{ j.titulo }}</td>
              <td>{{ j.plataforma }}</td>
              <td>{{ j.categoria?.nome }}</td>
              <td>{{ j.preco | currency:'BRL' }}</td>
              <td class="actions">
                <button class="btn" (click)="editar(j)">Editar</button>
                <button class="btn btn-danger" (click)="excluir(j)">Excluir</button>
              </td>
            </tr>
          }
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .form { margin-bottom: 1.5rem; }
    .cols { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 0 1rem; }
    .previa { display: block; width: 150px; aspect-ratio: 3 / 4; object-fit: cover;
              border-radius: 6px; border: 1px solid var(--line); margin-bottom: .75rem; }
  `]
})
export class AdminJogosComponent implements OnInit {
  private api = inject(ApiService);
  private toast = inject(ToastService);
  @ViewChild('arquivoInput') private arquivoInput?: ElementRef<HTMLInputElement>;

  jogos = signal<Jogo[]>([]);
  categorias = signal<Categoria[]>([]);
  erro = signal('');
  erroImagem = signal('');
  ok = signal('');
  salvando = signal(false);
  preview = signal<string | null>(null); // data URL local, só para exibir (nunca é enviado)
  form: Jogo = { ...VAZIO };
  arquivo: File | null = null;

  // expostos ao template
  readonly urlImagem = urlImagem;
  readonly aoFalharImagem = aoFalharImagem;

  ngOnInit() {
    this.api.listarCategorias().subscribe(c => this.categorias.set(c));
    this.carregar();
  }

  carregar() { this.api.listarJogos().subscribe(j => this.jogos.set(j)); }

  editar(j: Jogo) {
    this.limparArquivo();
    this.form = { ...j, categoriaId: j.categoria?.id ?? null };
    this.erro.set(''); this.ok.set('');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  cancelar() {
    this.limparArquivo();
    this.form = { ...VAZIO };
    this.erro.set(''); this.ok.set('');
  }

  aoEscolherArquivo(evento: Event) {
    const input = evento.target as HTMLInputElement;
    const arquivo = input.files?.[0] ?? null;

    this.erroImagem.set('');
    this.arquivo = null;
    this.preview.set(null);
    if (!arquivo) return; // o usuário cancelou o seletor

    const problema = validarImagemJpeg(arquivo);
    if (problema) {
      this.erroImagem.set(problema);
      input.value = ''; // descarta a seleção inválida
      return;
    }

    this.arquivo = arquivo;
    const leitor = new FileReader(); // só para pré-visualizar
    leitor.onload = () => this.preview.set(leitor.result as string);
    leitor.readAsDataURL(arquivo);
  }

  salvar() {
    this.ok.set('');
    if (!this.form.titulo.trim() || !this.form.categoriaId || this.form.preco <= 0) {
      this.erro.set('Informe título, categoria e um preço maior que zero.');
      return;
    }
    if (this.erroImagem()) {
      this.erro.set('Corrija o arquivo de imagem (ou remova a seleção) antes de salvar.');
      return;
    }
    this.erro.set('');
    this.salvando.set(true);

    const dados: JogoFormDados = {
      titulo: this.form.titulo, descricao: this.form.descricao, preco: this.form.preco,
      plataforma: this.form.plataforma, categoriaId: this.form.categoriaId
    };
    const req = this.form.id
      ? this.api.editarJogo(this.form.id, dados, this.arquivo)
      : this.api.criarJogo(dados, this.arquivo);

    req.subscribe({
      next: () => {
        this.ok.set('Jogo salvo.');
        this.toast.sucesso(this.form.id ? 'Jogo atualizado com sucesso!' : 'Jogo cadastrado com sucesso!');
        this.limparArquivo();
        this.form = { ...VAZIO };
        this.salvando.set(false);
        this.carregar();
      },
      error: (e: HttpErrorResponse) => {
        console.error('Falha ao salvar jogo:', e); // agora o detalhe aparece no console
        this.erro.set(this.mensagemDeErro(e));
        this.toast.erro(this.mensagemDeErro(e));
        this.salvando.set(false);
      }
    });
  }

  excluir(j: Jogo) {
    if (!j.id || !confirm(`Excluir "${j.titulo}"?`)) return;
    this.api.excluirJogo(j.id).subscribe({
      next: () => { this.carregar(); this.toast.sucesso('Jogo excluído com sucesso!'); },
      error: () => { this.erro.set('Não foi possível excluir. O jogo pode estar em algum pedido.'); this.toast.erro('Não foi possível excluir. O jogo pode estar em algum pedido.'); }
    });
  }

  private limparArquivo() {
    this.arquivo = null;
    this.preview.set(null);
    this.erroImagem.set('');
    if (this.arquivoInput) this.arquivoInput.nativeElement.value = '';
  }

  /** Usa a mensagem específica do @ControllerAdvice ({ mensagem }) quando existir. */
  private mensagemDeErro(e: HttpErrorResponse): string {
    if (e.status === 0) {
      return 'Sem conexão com a API. Verifique se o backend está rodando em http://localhost:8080.';
    }
    const msg = e.error?.mensagem;
    if (typeof msg === 'string' && msg) return msg;
    if (e.status === 413) return 'A imagem excede o tamanho máximo de 5 MB.';
    return `Não foi possível salvar o jogo (HTTP ${e.status}).`;
  }
}