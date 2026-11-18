// loja_de_jogo_back/src/main/java/br/com/gamestore/pi_3/exception/ApiExceptionHandler.java
package br.com.gamestore.pi_3.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    /** Corpo padrão de erro. O Angular lê "mensagem" e, se quiser, "campos". */
    public record ErroResposta(int status, String erro, String mensagem,
                               Map<String, String> campos, Instant timestamp) {}

    private static ResponseEntity<ErroResposta> resposta(HttpStatus status, String mensagem,
                                                         Map<String, String> campos) {
        return ResponseEntity.status(status)
                .body(new ErroResposta(status.value(), status.getReasonPhrase(), mensagem, campos, Instant.now()));
    }

    /** Arquivo vazio, extensão/MIME/conteúdo inválido, falha de gravação. */
    @ExceptionHandler(UploadException.class)
    public ResponseEntity<ErroResposta> upload(UploadException ex) {
        if (ex.getStatus().is5xxServerError()) {
            log.error("Falha ao gravar imagem: {}", ex.getMessage(), ex);
        } else {
            log.warn("Imagem rejeitada ({}): {}", ex.getStatus().value(), ex.getMessage());
        }
        return resposta(ex.getStatus(), ex.getMessage(), Map.of("imagem", ex.getMessage()));
    }

    /** Estourou spring.servlet.multipart.max-file-size / max-request-size. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResposta> tamanhoExcedido(MaxUploadSizeExceededException ex) {
        String msg = "A imagem excede o tamanho máximo de 5 MB.";
        log.warn("Upload acima do limite configurado: {}", ex.getMessage());
        return resposta(HttpStatus.PAYLOAD_TOO_LARGE, msg, Map.of("imagem", msg));
    }

    /** @Valid falhou (MethodArgumentNotValidException é um BindException) ou conversão (preco="abc"). */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErroResposta> validacao(BindException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError e : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(e.getField(), mensagemDe(e));
        }
        String msg = campos.isEmpty() ? "Dados inválidos." : String.join(" ", campos.values());
        log.warn("Validação falhou: {}", campos);
        return resposta(HttpStatus.BAD_REQUEST, msg, campos);
    }

    /** O erro atual do projeto: valor maior que a coluna (VARCHAR 255) ou violação de constraint. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResposta> integridade(DataIntegrityViolationException ex) {
        log.error("Violação de integridade no banco. CAUSA EXATA: {}", ex.getMostSpecificCause().getMessage(), ex);
        return resposta(HttpStatus.BAD_REQUEST,
                "Não foi possível gravar: algum campo excede o tamanho permitido ou viola uma regra do banco.",
                Map.of());
    }

    /** Mantém o texto (ex.: "Categoria inexistente", "Jogo não encontrado") que o Boot esconderia. */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroResposta> statusExplicito(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String msg = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        log.warn("{} - {}", status.value(), msg);
        return resposta(status, msg, Map.of());
    }

    private static String mensagemDe(FieldError e) {
        return e.isBindingFailure()
                ? "Valor inválido para o campo '" + e.getField() + "'."
                : Objects.requireNonNullElse(e.getDefaultMessage(), "Valor inválido.");
    }
}