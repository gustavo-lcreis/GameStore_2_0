package br.com.gamestore.pi_3.security;

import br.com.gamestore.pi_3.model.Usuario;
import br.com.gamestore.pi_3.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock UsuarioRepository usuarios;

    TokenService tokens = new TokenService(8);
    AuthInterceptor interceptor;

    private final Usuario admin = new Usuario(1L, "Admin", "admin", "x", "ROLE_ADMIN");
    private final Usuario cliente = new Usuario(2L, "Ana", "ana@x.com", "x", "ROLE_USER");

    @BeforeEach
    void setUp() {
        interceptor = new AuthInterceptor(tokens, usuarios);
    }

    private MockHttpServletRequest request(String metodo, String uri, String token) {
        MockHttpServletRequest req = new MockHttpServletRequest(metodo, uri);
        if (token != null) req.addHeader("Authorization", "Bearer " + token);
        return req;
    }

    private boolean chamar(MockHttpServletRequest req) {
        return interceptor.preHandle(req, new MockHttpServletResponse(), new Object());
    }

    private static void assertStatus(Throwable t, HttpStatus esperado) {
        assertThat(t).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode()).isEqualTo(esperado));
    }

    @Test
    void semTokenRetorna401() {
        assertThatThrownBy(() -> chamar(request("GET", "/api/admin/vendas", null)))
                .satisfies(t -> assertStatus(t, HttpStatus.UNAUTHORIZED));
    }

    @Test
    void tokenInvalidoRetorna401() {
        assertThatThrownBy(() -> chamar(request("GET", "/api/admin/vendas", "inventado")))
                .satisfies(t -> assertStatus(t, HttpStatus.UNAUTHORIZED));
    }

    @Test
    void tokenDeUsuarioJaExcluidoRetorna401() {
        String token = tokens.emitir(2L);
        when(usuarios.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chamar(request("POST", "/api/pedidos", token)))
                .satisfies(t -> assertStatus(t, HttpStatus.UNAUTHORIZED));
    }

    @Test
    void clienteNaAreaAdminRetorna403() {
        String token = tokens.emitir(2L);
        when(usuarios.findById(2L)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> chamar(request("DELETE", "/api/admin/usuarios/5", token)))
                .satisfies(t -> assertStatus(t, HttpStatus.FORBIDDEN));
    }

    @Test
    void adminPassaEFicaDisponivelNaRequest() {
        String token = tokens.emitir(1L);
        when(usuarios.findById(1L)).thenReturn(Optional.of(admin));
        MockHttpServletRequest req = request("DELETE", "/api/admin/usuarios/5", token);

        assertThat(chamar(req)).isTrue();
        assertThat(req.getAttribute(AuthInterceptor.ATTR_USUARIO)).isSameAs(admin);
    }

    @Test
    void clientePodeFinalizarPedido() {
        String token = tokens.emitir(2L);
        when(usuarios.findById(2L)).thenReturn(Optional.of(cliente));

        assertThat(chamar(request("POST", "/api/pedidos", token))).isTrue();
    }

    @Test
    void tokenRevogadoDeixaDeValer() {
        String token = tokens.emitir(1L);
        tokens.revogar(token);

        assertThatThrownBy(() -> chamar(request("GET", "/api/admin/vendas", token)))
                .satisfies(t -> assertStatus(t, HttpStatus.UNAUTHORIZED));
    }

    @Test
    void preflightDeCorsNaoExigeToken() {
        assertThat(chamar(request("OPTIONS", "/api/admin/vendas", null))).isTrue();
    }
}
