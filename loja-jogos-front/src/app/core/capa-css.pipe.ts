// loja-jogos-front/src/app/core/capa-css.pipe.ts
import { Pipe, PipeTransform } from '@angular/core';
import { urlImagem } from './imagem';

const GRADIENTE = 'linear-gradient(160deg, #2b2550, #5b3df5)';

@Pipe({ name: 'capaCss', standalone: true })
export class CapaCssPipe implements PipeTransform {
  transform(caminho?: string | null): string | null {
    const url = urlImagem(caminho);
    return url ? `url("${url.replace(/"/g, '%22')}"), ${GRADIENTE}` : null;
  }
}