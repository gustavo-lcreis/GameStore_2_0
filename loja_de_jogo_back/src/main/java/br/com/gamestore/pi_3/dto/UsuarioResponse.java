// loja_de_jogo_back/src/main/java/br/com/gamestore/pi_3/dto/UsuarioResponse.java
package br.com.gamestore.pi_3.dto;

import br.com.gamestore.pi_3.model.Usuario;

/** Formato que o Angular espera (models.ts -> Usuario). Nunca inclui a senha. */
public record UsuarioResponse(Long id, String nome, String email, String role, String tipo) {

    public static UsuarioResponse de(Usuario u) {
        boolean admin = "ROLE_ADMIN".equals(u.getRole());
        String nome = u.getNome() != null ? u.getNome() : u.getUsername();
        return new UsuarioResponse(u.getId(), nome, u.getUsername(), u.getRole(), admin ? "ADMIN" : "CLIENTE");
    }
}