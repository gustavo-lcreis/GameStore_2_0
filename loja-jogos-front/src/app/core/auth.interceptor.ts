import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { ToastService } from './toast.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const toast = inject(ToastService);

  const token = auth.token;
  const requisicao = token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(requisicao).pipe(
    catchError((erro: unknown) => {
      // 401 fora do login = sessão expirada/inexistente (as sessões do back ficam em memória e somem ao reiniciar).
      // Antes o front continuava "logado" e as telas falhavam em silêncio (ex.: "Meus pedidos" vazio).
      const ehLogin = req.url.includes('/auth/login');
      if (erro instanceof HttpErrorResponse && erro.status === 401 && !ehLogin && auth.isLoggedIn) {
        auth.logout();
        toast.aviso('Sua sessão expirou. Entre novamente.');
        router.navigate(['/login']);
      }
      return throwError(() => erro);
    })
  );
};
