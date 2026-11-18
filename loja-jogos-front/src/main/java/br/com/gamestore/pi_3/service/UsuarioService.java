package br.com.gamestore.pi_3.service;

import br.com.gamestore.pi_3.model.AuditoriaAdmin;
import br.com.gamestore.pi_3.model.Usuario;
import br.com.gamestore.pi_3.repository.AuditoriaAdminRepository;
import br.com.gamestore.pi_3.repository.PedidoRepository;
import br.com.gamestore.pi_3.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class UsuarioService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UsuarioRepository usuarioRepository;
    private final PedidoRepository pedidoRepository;
    private final AuditoriaAdminRepository auditoria;

    public UsuarioService(UsuarioRepository usuarioRepository, PedidoRepository pedidoRepository,
                          AuditoriaAdminRepository auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.pedidoRepository = pedidoRepository;
        this.auditoria = auditoria;
    }

    /**
     * Exclui o CLIENTE MANTENDO os pedidos dele: a referência (usuario_id) vira null e o nome/e-mail
     * do cliente continuam gravados no próprio pedido. Assim o histórico de vendas não é perdido
     * e a exclusão nunca é bloqueada por pedidos antigos.
     *
     * Regras: não exclui a si mesmo; não exclui contas ADMIN (evita ficar sem administrador e escalada lateral);
     * grava auditoria (quem, quando, qual cliente) NA MESMA transação: ou exclui e audita, ou nada acontece.
     *
     * @return quantos pedidos foram preservados
     */
    @Transactional
    public int excluir(Long id, Usuario solicitante) {
        if (solicitante != null && id.equals(solicitante.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você não pode excluir a sua própria conta.");
        }
        Usuario alvo = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        if (ROLE_ADMIN.equals(alvo.getRole())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Contas de administrador não podem ser excluídas por esta tela.");
        }

        int preservados = pedidoRepository.desvincularUsuario(id);
        usuarioRepository.delete(alvo);
        usuarioRepository.flush(); // qualquer violação de integridade estoura aqui, dentro do método
        auditoria.save(AuditoriaAdmin.exclusaoUsuario(solicitante, alvo, preservados));

        log.info("Usuário {} ({}) excluído por {} ({} pedido(s) preservados)",
                id, alvo.getUsername(), solicitante != null ? solicitante.getUsername() : "?", preservados);
        return preservados;
    }
}
