package br.com.gamestore.pi_3.security;

import br.com.gamestore.pi_3.model.Usuario;
import br.com.gamestore.pi_3.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * Exige "Authorization: Bearer <token>" nas rotas em que está registrado (ver WebConfig):
 *  - /api/pedidos/**  -> qualquer usuário autenticado
 *  - /api/admin/**    -> somente ROLE_ADMIN
 * As exceções lançadas aqui passam pelo ApiExceptionHandler e saem no formato padrão de erro.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** Nome do atributo da request com o Usuario autenticado (use com @RequestAttribute). */
    public static final String ATTR_USUARIO = "usuarioAutenticado";

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String PREFIXO_BEARER = "Bearer ";

    private final TokenService tokens;
    private final UsuarioRepository usuarios;

    public AuthInterceptor(TokenService tokens, UsuarioRepository usuarios) {
        this.tokens = tokens;
        this.usuarios = usuarios;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; // preflight de CORS

        String token = extrairToken(request.getHeader("Authorization"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária."));

        // Busca o usuário a cada request: papel sempre atual, e token de usuário excluído deixa de valer.
        Usuario usuario = tokens.resolver(token)
                .flatMap(usuarios::findById)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Sessão inválida ou expirada. Faça login novamente."));

        if (request.getRequestURI().startsWith("/api/admin") && !ROLE_ADMIN.equals(usuario.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito a administradores.");
        }

        request.setAttribute(ATTR_USUARIO, usuario);
        return true;
    }

    public static Optional<String> extrairToken(String authorization) {
        if (authorization == null || !authorization.startsWith(PREFIXO_BEARER)) return Optional.empty();
        String token = authorization.substring(PREFIXO_BEARER.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
