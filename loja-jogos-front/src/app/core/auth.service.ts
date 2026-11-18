// loja-jogos-front/src/app/core/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, map, tap } from 'rxjs';
import { LoginResposta, Usuario } from '../models';

type RespostaLogin = Usuario | LoginResposta;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly USER_KEY = 'user';
  private readonly TOKEN_KEY = 'token';
  private currentUserSubject = new BehaviorSubject<Usuario | null>(this.lerUsuario());
  readonly currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {}

  login(credentials: { username: string; password: string }): Observable<Usuario> {
    return this.http.post<RespostaLogin>('/api/auth/login', credentials).pipe(
      map(resposta => this.lerResposta(resposta)),
      tap(usuario => this.guardarSessao(usuario))
    );
  }

  /**
   * Cria a conta (papel CLIENTE) e já deixa o usuário logado. O back devolve o token junto com o usuário:
   * sem ele o usuário parecia logado no front, mas todo POST /api/pedidos voltava 401.
   */
  registrar(dados: { nome: string; email: string; senha: string }): Observable<Usuario> {
    return this.http.post<RespostaLogin>('/api/auth/registro', dados).pipe(
      map(resposta => this.lerResposta(resposta)),
      tap(usuario => this.guardarSessao(usuario))
    );
  }

  /** Aceita { token, usuario } OU o usuário "achatado" com token (formato atual do back). Guarda o token à parte. */
  private lerResposta(resposta: RespostaLogin): Usuario {
    const payload = resposta as LoginResposta;
    const usuario = payload.usuario ?? resposta as Usuario;
    if (!usuario || typeof usuario !== 'object') {
      throw new Error('Resposta de autenticação inválida.');
    }
    if (payload.token) localStorage.setItem(this.TOKEN_KEY, payload.token);
    else localStorage.removeItem(this.TOKEN_KEY);
    const semToken: Record<string, unknown> = { ...usuario };
    delete semToken['token']; // o token não precisa ficar duplicado dentro do objeto do usuário
    return semToken as unknown as Usuario;
  }

  logout(): void {
    localStorage.removeItem(this.USER_KEY);
    localStorage.removeItem(this.TOKEN_KEY);
    this.currentUserSubject.next(null);
  }

  get isLoggedIn(): boolean { return !!this.currentUserSubject.value; }
  logado(): boolean { return this.isLoggedIn; }

  get isAdmin(): boolean {
    const user = this.currentUserSubject.value;
    return user?.role === 'ROLE_ADMIN' || user?.tipo === 'ADMIN';
  }
  admin(): boolean { return this.isAdmin; }

  get token(): string | null { return localStorage.getItem(this.TOKEN_KEY); }

  private guardarSessao(usuario: Usuario): void {
    localStorage.setItem(this.USER_KEY, JSON.stringify(usuario));
    this.currentUserSubject.next(usuario);
  }

  private lerUsuario(): Usuario | null {
    try {
      const salvo = localStorage.getItem(this.USER_KEY);
      return salvo ? JSON.parse(salvo) as Usuario : null;
    } catch {
      localStorage.removeItem(this.USER_KEY);
      return null;
    }
  }
}