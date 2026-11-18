package br.com.gamestore.pi_3.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sessões em memória: token opaco aleatório -> id do usuário. Simples e suficiente para este projeto,
 * mas os tokens se perdem ao reiniciar a aplicação (o usuário só precisa logar de novo).
 */
@Service
public class TokenService {

    private record Sessao(Long usuarioId, Instant expiraEm) {}

    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Duration validade;

    public TokenService(@Value("${app.auth.token-ttl-hours:8}") long validadeEmHoras) {
        this.validade = Duration.ofHours(validadeEmHoras);
    }

    public String emitir(Long usuarioId) {
        Instant agora = Instant.now();
        sessoes.values().removeIf(s -> s.expiraEm().isBefore(agora)); // limpeza oportunista

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessoes.put(token, new Sessao(usuarioId, agora.plus(validade)));
        return token;
    }

    /** Id do usuário dono do token, se ele existir e não tiver expirado. */
    public Optional<Long> resolver(String token) {
        if (token == null) return Optional.empty();
        Sessao s = sessoes.get(token);
        if (s == null) return Optional.empty();
        if (s.expiraEm().isBefore(Instant.now())) {
            sessoes.remove(token);
            return Optional.empty();
        }
        return Optional.of(s.usuarioId());
    }

    public void revogar(String token) {
        if (token != null) sessoes.remove(token);
    }
}
