// loja_de_jogo_back/src/main/java/br/com/gamestore/pi_3/controller/AuthController.java
package br.com.gamestore.pi_3.controller;

import br.com.gamestore.pi_3.dto.LoginResponse;
import br.com.gamestore.pi_3.dto.RegistroRequest;
import br.com.gamestore.pi_3.model.Usuario;
import br.com.gamestore.pi_3.security.AuthInterceptor;
import br.com.gamestore.pi_3.security.TokenService;
import br.com.gamestore.pi_3.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenService tokenService;

    public AuthController(AuthService authService, TokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return authService.autenticar(request.getUsername(), request.getPassword())
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(LoginResponse.de(u, tokenService.emitir(u.getId()))))
                .orElseGet(() -> ResponseEntity.status(401).body("Credenciais inválidas"));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthInterceptor.extrairToken(authorization).ifPresent(tokenService::revogar);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/registro")
    public ResponseEntity<LoginResponse> registrar(@Valid @RequestBody RegistroRequest req) {
        Usuario novo = authService.registrar(req);
        // Já devolve o token: o front deixa o usuário logado após o cadastro, e sem token todo POST /api/pedidos daria 401.
        return ResponseEntity.status(HttpStatus.CREATED).body(LoginResponse.de(novo, tokenService.emitir(novo.getId())));
    }

    public static class LoginRequest {

        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}