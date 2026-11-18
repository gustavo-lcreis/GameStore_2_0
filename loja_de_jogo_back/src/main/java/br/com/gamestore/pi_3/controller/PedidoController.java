package br.com.gamestore.pi_3.controller;

import br.com.gamestore.pi_3.dto.PedidoRequest;
import br.com.gamestore.pi_3.dto.PedidoResponse;
import br.com.gamestore.pi_3.model.Pedido;
import br.com.gamestore.pi_3.model.Usuario;
import br.com.gamestore.pi_3.security.AuthInterceptor;
import br.com.gamestore.pi_3.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    /** UUID, ULID, etc. Limita tamanho e alfabeto: a chave é gravada no banco e nunca interpretada. */
    private static final Pattern CHAVE_VALIDA = Pattern.compile("[A-Za-z0-9_-]{8,64}");

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    /**
     * Finaliza a compra simulada do usuário autenticado (o cliente vem do token, nunca do corpo).
     * Header opcional "Idempotency-Key": repetir o POST com a mesma chave devolve o pedido já criado (200),
     * em vez de criar um segundo (201).
     */
    @PostMapping
    public ResponseEntity<PedidoResponse> finalizar(
            @Valid @RequestBody PedidoRequest req,
            @RequestHeader(value = "Idempotency-Key", required = false) String chaveBruta,
            @RequestAttribute(AuthInterceptor.ATTR_USUARIO) Usuario usuario) {

        String chave = validarChave(chaveBruta);
        if (chave != null) {
            var existente = pedidoService.buscarPorChave(usuario, chave);
            if (existente.isPresent()) return ResponseEntity.ok(existente.get());
        }
        try {
            Pedido pedido = pedidoService.finalizar(usuario, req, chave);
            return ResponseEntity.status(HttpStatus.CREATED).body(PedidoResponse.de(pedido));
        } catch (DataIntegrityViolationException e) {
            // Corrida: outro request com a mesma chave gravou primeiro (constraint única). A transação deste
            // já foi desfeita; devolvemos o pedido vencedor. Sem chave, o erro é outro: segue para o handler global.
            if (chave == null) throw e;
            return pedidoService.buscarPorChave(usuario, chave)
                    .map(ResponseEntity::ok)
                    .orElseThrow(() -> e);
        }
    }

    /** "Meus pedidos": SOMENTE os do usuário do token (nunca aceita id de usuário na URL). */
    @GetMapping("/meus")
    public List<PedidoResponse> meus(@RequestAttribute(AuthInterceptor.ATTR_USUARIO) Usuario usuario) {
        return pedidoService.meusPedidos(usuario);
    }

    private static String validarChave(String chave) {
        if (chave == null || chave.isBlank()) return null;
        String c = chave.trim();
        if (!CHAVE_VALIDA.matcher(c).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key inválida: use de 8 a 64 caracteres (letras, números, '-' ou '_').");
        }
        return c;
    }
}
