// loja_de_jogo_back/src/main/java/br/com/gamestore/pi_3/dto/JogoRequest.java
package br.com.gamestore.pi_3.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Campos de texto do multipart/form-data. A imagem chega como arquivo (part "imagem"),
 * por isso não existe mais um campo "imagem" aqui.
 */
public record JogoRequest(
        @NotBlank(message = "Informe o título.")
        @Size(max = 255, message = "O título deve ter no máximo 255 caracteres.")
        String titulo,

        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
        String descricao,

        @NotNull(message = "Informe o preço.")
        @Positive(message = "O preço deve ser maior que zero.")
        Double preco,

        @Size(max = 255, message = "A plataforma deve ter no máximo 255 caracteres.")
        String plataforma,

        Long categoriaId
) {}