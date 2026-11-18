// loja_de_jogo_back/src/main/java/br/com/gamestore/pi_3/exception/UploadException.java
package br.com.gamestore.pi_3.exception;

import org.springframework.http.HttpStatus;

public class UploadException extends RuntimeException {

    private final HttpStatus status;

    public UploadException(HttpStatus status, String mensagem) {
        super(mensagem);
        this.status = status;
    }

    public UploadException(HttpStatus status, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}