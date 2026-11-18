package br.com.gamestore.pi_3.service;

import br.com.gamestore.pi_3.model.Pedido;
import br.com.gamestore.pi_3.model.Usuario;
import br.com.gamestore.pi_3.repository.PedidoRepository;
import br.com.gamestore.pi_3.repository.UsuarioRepository;
import br.com.gamestore.pi_3.support.PedidoTestFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Roda contra um H2 real (com as FKs), para provar que a exclusão não é barrada pelos pedidos. */
@DataJpaTest
@Import(UsuarioService.class)
class UsuarioServiceIntegrationTest {

    @Autowired UsuarioService service;
    @Autowired UsuarioRepository usuarios;
    @Autowired PedidoRepository pedidos;
    @Autowired TestEntityManager em;

    private Usuario novoUsuario(String nome, String email, String role) {
        return usuarios.save(new Usuario(null, nome, email, "hash", role));
    }

    @Test
    void excluirUsuarioComPedidosMantemOsPedidosSemReferencia() {
        Usuario admin = novoUsuario("Admin", "admin@x.com", "ROLE_ADMIN");
        Usuario cliente = novoUsuario("Ana", "ana@x.com", "ROLE_USER");
        Pedido p1 = pedidos.save(PedidoTestFactory.pedido(cliente, LocalDateTime.of(2026, 11, 10, 10, 0), "100.00"));
        Pedido p2 = pedidos.save(PedidoTestFactory.pedido(cliente, LocalDateTime.of(2026, 11, 11, 10, 0), "50.00"));

        int preservados = service.excluir(cliente.getId(), admin);
        em.flush();
        em.clear();

        assertThat(preservados).isEqualTo(2);
        assertThat(usuarios.findById(cliente.getId())).isEmpty();
        for (Long id : new Long[]{p1.getId(), p2.getId()}) {
            Pedido salvo = pedidos.findById(id).orElseThrow();
            assertThat(salvo.getUsuario()).isNull();
            assertThat(salvo.getClienteEmail()).isEqualTo("ana@x.com"); // histórico continua legível
            assertThat(salvo.getItens()).hasSize(1);
        }
    }

    @Test
    void excluirUsuarioSemPedidos() {
        Usuario admin = novoUsuario("Admin", "admin@x.com", "ROLE_ADMIN");
        Usuario cliente = novoUsuario("Bia", "bia@x.com", "ROLE_USER");

        int preservados = service.excluir(cliente.getId(), admin);
        em.flush();
        em.clear();

        assertThat(preservados).isZero();
        assertThat(usuarios.findById(cliente.getId())).isEmpty();
    }

    @Test
    void naoPermiteExcluirAPropriaConta() {
        Usuario admin = novoUsuario("Admin", "admin@x.com", "ROLE_ADMIN");

        assertThatThrownBy(() -> service.excluir(admin.getId(), admin))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(usuarios.findById(admin.getId())).isPresent();
    }

    @Test
    void usuarioInexistenteRetorna404() {
        Usuario admin = novoUsuario("Admin", "admin@x.com", "ROLE_ADMIN");

        assertThatThrownBy(() -> service.excluir(9999L, admin))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
