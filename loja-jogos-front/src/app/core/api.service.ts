// loja-jogos-front/src/app/core/api.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../environments/environment';
import {
  Agrupamento, Categoria, DashboardResposta, ExclusaoUsuarioResposta, ItemCarrinho, Jogo, JogoFormDados,
  Page, Pedido, Usuario, VendasResposta
} from '../models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private http = inject(HttpClient);

  // Base única da API: trocar de ambiente = mudar só o environment.ts
  private readonly API = environment.apiUrl;
  private readonly ADMIN_ROUTE = `${this.API}/admin`;

  // Jogos
  listarJogos(busca?: string, categoriaId?: number | null): Observable<Jogo[]> {
    let params = new HttpParams();
    if (busca) params = params.set('busca', busca);
    if (categoriaId) params = params.set('categoria', categoriaId);
    return this.http.get<Jogo[] | Page<Jogo>>(`${this.API}/jogos`, { params }).pipe(
      // Aceita array puro (formato atual do back) OU Page { content: [...] }.
      map(r => Array.isArray(r) ? r : (r?.content ?? []))
    );
  }
  buscarJogo(id: number) { return this.http.get<Jogo>(`${this.API}/jogos/${id}`); }

  // multipart/form-data: campos de texto + arquivo "imagem" (opcional)
  criarJogo(dados: JogoFormDados, imagem?: File | null) {
    return this.http.post<Jogo>(`${this.ADMIN_ROUTE}/jogos`, this.montarForm(dados, imagem));
  }
  editarJogo(id: number, dados: JogoFormDados, imagem?: File | null) {
    return this.http.put<Jogo>(`${this.ADMIN_ROUTE}/jogos/${id}`, this.montarForm(dados, imagem));
  }
  excluirJogo(id: number) { return this.http.delete<void>(`${this.ADMIN_ROUTE}/jogos/${id}`); }

  // Categorias
  listarCategorias() { return this.http.get<Categoria[]>(`${this.API}/categorias`); }

  // Pedidos
  /** A chave de idempotência faz o back devolver o MESMO pedido se este POST for repetido (duplo clique, retry). */
  criarPedido(itens: ItemCarrinho[], chaveIdempotencia?: string) {
    const headers = chaveIdempotencia ? new HttpHeaders({ 'Idempotency-Key': chaveIdempotencia }) : undefined;
    return this.http.post<Pedido>(`${this.API}/pedidos`, {
      itens: itens.map(i => ({ jogoId: i.jogo.id, quantidade: i.quantidade }))
    }, { headers });
  }
  meusPedidos() { return this.http.get<Pedido[]>(`${this.API}/pedidos/meus`); }

  // Admin
  vendas(page = 0, size = 20, inicio?: string, fim?: string) {
    let params = new HttpParams().set('page', page).set('size', size);
    if (inicio) params = params.set('inicio', inicio);
    if (fim) params = params.set('fim', fim);
    return this.http.get<VendasResposta>(`${this.ADMIN_ROUTE}/vendas`, { params });
  }
  dashboard(inicio: string, fim: string, agrupamento: Agrupamento, top = 5) {
    const params = new HttpParams().set('inicio', inicio).set('fim', fim)
      .set('agrupamento', agrupamento).set('top', top);
    return this.http.get<DashboardResposta>(`${this.ADMIN_ROUTE}/dashboard`, { params });
  }
  listarUsuarios() { return this.http.get<Usuario[]>(`${this.ADMIN_ROUTE}/usuarios`); }
  excluirUsuario(id: number) { return this.http.delete<ExclusaoUsuarioResposta>(`${this.ADMIN_ROUTE}/usuarios/${id}`); }

  /** NÃO defina Content-Type manualmente: o navegador acrescenta o boundary do multipart. */
  private montarForm(d: JogoFormDados, imagem?: File | null): FormData {
    const form = new FormData();
    form.append('titulo', d.titulo.trim());
    form.append('descricao', d.descricao ?? '');
    form.append('preco', String(d.preco));
    form.append('plataforma', d.plataforma ?? '');
    if (d.categoriaId != null) form.append('categoriaId', String(d.categoriaId));
    if (imagem) form.append('imagem', imagem, imagem.name);
    return form;
  }
}