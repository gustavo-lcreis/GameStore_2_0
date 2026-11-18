// loja_de_jogo_back/src/main/java/br/com/gamestore/pi_3/service/AuthService.java
package br.com.gamestore.pi_3.service;

import br.com.gamestore.pi_3.dto.RegistroRequest;
import br.com.gamestore.pi_3.model.Usuario;
import br.com.gamestore.pi_3.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

@Slf4j
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Optional<Usuario> autenticar(String username, String password) {
        if (username == null || password == null) return Optional.empty();
        return usuarioRepository.findByUsername(normalizar(username))
                .filter(u -> senhaConfere(u, password));
    }

    public Usuario registrar(RegistroRequest req) {
        String email = normalizar(req.email());
        if (usuarioRepository.findByUsername(email).isPresent()) {
            throw emailEmUso();
        }
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setUsername(email);
        u.setPassword(encoder.encode(req.senha()));
        u.setRole("ROLE_USER"); // o papel nunca vem do cliente
        try {
            Usuario salvo = usuarioRepository.save(u);
            log.info("Usuário cadastrado: id={} email={}", salvo.getId(), email);
            return salvo;
        } catch (DataIntegrityViolationException e) { // dois cadastros simultâneos com o mesmo e-mail
            throw emailEmUso();
        }
    }

    private boolean senhaConfere(Usuario u, String informada) {
        String salva = u.getPassword();
        if (salva == null) return false;
        if (salva.startsWith("$2")) return encoder.matches(informada, salva); // já é hash BCrypt
        // legado (admin do seed, gravado em texto puro): confere e já converte para hash
        if (salva.equals(informada)) {
            u.setPassword(encoder.encode(informada));
            usuarioRepository.save(u);
            log.info("Senha do usuário '{}' convertida para BCrypt", u.getUsername());
            return true;
        }
        return false;
    }

    private static String normalizar(String s) {
        return s.trim().toLowerCase(Locale.ROOT);
    }

    private static ResponseStatusException emailEmUso() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
    }
}