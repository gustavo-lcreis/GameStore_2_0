package br.com.gamestore.pi_3.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Só o id e a quantidade vêm do cliente. Preço e total são SEMPRE calculados no servidor. */
public record ItemPedidoRequest(
        @NotNull(message = "Informe o jogo do item.")
        Long jogoId,

        @NotNull(message = "Informe a quantidade.")
        @Min(value = 1, message = "A quantidade deve ser no mínimo 1.")
        @Max(value = 99, message = "A quantidade máxima por jogo é 99.")
        Integer quantidade
) {}
