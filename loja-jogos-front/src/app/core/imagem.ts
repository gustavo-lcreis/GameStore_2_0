// loja-jogos-front/src/app/core/imagem.ts
import { environment } from '../../environments/environment';

export const LIMITE_IMAGEM_BYTES = 5 * 1024 * 1024; // 5 MB (igual ao back)
export const IMAGEM_FALLBACK = '/assets/placeholder-jogo.svg';

/** Valida o arquivo escolhido. Devolve a mensagem de erro, ou '' se estiver ok. */
export function validarImagemJpeg(arquivo: File): string {
  if (arquivo.size === 0) return 'O arquivo selecionado está vazio.';
  if (!/\.jpe?g$/i.test(arquivo.name)) return 'Extensão inválida. Selecione um arquivo .jpg ou .jpeg.';
  if (arquivo.type !== 'image/jpeg') return 'Tipo de arquivo inválido. Apenas imagens JPEG (image/jpeg) são aceitas.';
  if (arquivo.size > LIMITE_IMAGEM_BYTES) {
    const mb = (arquivo.size / (1024 * 1024)).toFixed(1);
    return `A imagem tem ${mb} MB e o limite é 5 MB.`;
  }
  return '';
}

/** Caminho vindo da API (/assets/jogos/x.jpg) -> URL completa (apiUrl + caminho). */
export function urlImagem(caminho?: string | null): string | null {
  if (!caminho) return null;
  if (/^(https?:|data:|blob:)/i.test(caminho)) return caminho;
  return `${environment.apiUrl}${caminho.startsWith('/') ? '' : '/'}${caminho}`;
}

/** Handler de (error) em <img>: troca pelo placeholder uma única vez (evita loop). */
export function aoFalharImagem(evento: Event): void {
  const img = evento.target as HTMLImageElement;
  if (img.dataset['fallback']) return;
  img.dataset['fallback'] = '1';
  img.src = IMAGEM_FALLBACK;
}