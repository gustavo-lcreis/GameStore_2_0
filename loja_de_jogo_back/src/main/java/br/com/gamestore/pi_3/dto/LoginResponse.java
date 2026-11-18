package br.com.gamestore.pi_3.dto;

import br.com.gamestore.pi_3.model.Usuario;

/** Mesmos campos de UsuarioResponse + token. O front antigo continua funcionando: só ignora o campo novo. */
public record LoginResponse(Long id, String nome, String email, String role, String tipo, String token) {

    public static LoginResponse de(Usuario u, String token) {
        UsuarioResponse r = UsuarioResponse.de(u);
        return new LoginResponse(r.id(), r.nome(), r.email(), r.role(), r.tipo(), token);
    }
}
