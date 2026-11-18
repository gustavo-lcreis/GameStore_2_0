package br.com.gamestore.pi_3.controller;

import br.com.gamestore.pi_3.dto.ExclusaoUsuarioResponse;
import br.com.gamestore.pi_3.dto.UsuarioResponse;
import br.com.gamestore.pi_3.model.Usuario;
import br.com.gamestore.pi_3.repository.UsuarioRepository;
import br.com.gamestore.pi_3.security.AuthInterceptor;
import br.com.gamestore.pi_3.service.UsuarioService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminUsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    public AdminUsuarioController(UsuarioRepository usuarioRepository, UsuarioService usuarioService) {
        this.usuarioRepository = usuarioRepository;
        this.usuarioService = usuarioService;
    }

    /** Lista todos os usuários (sem senha) para o painel de administração. */
    @GetMapping("/usuarios")
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll().stream()
                .map(UsuarioResponse::de)
                .toList();
    }

    /** Exclui um usuário mantendo os pedidos dele (ver UsuarioService). Só ADMIN chega aqui. */
    @DeleteMapping("/usuarios/{id}")
    public ExclusaoUsuarioResponse excluir(
            @PathVariable Long id,
            @RequestAttribute(AuthInterceptor.ATTR_USUARIO) Usuario solicitante) {
        int preservados = usuarioService.excluir(id, solicitante);
        String mensagem = preservados == 0
                ? "Usuário excluído com sucesso."
                : "Usuário excluído. " + preservados + " pedido(s) foram mantidos no histórico de vendas.";
        return new ExclusaoUsuarioResponse(mensagem, preservados);
    }
}
