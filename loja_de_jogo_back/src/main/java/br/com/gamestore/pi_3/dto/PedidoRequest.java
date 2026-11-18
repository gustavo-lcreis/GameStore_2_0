package br.com.gamestore.pi_3.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PedidoRequest(
        @NotEmpty(message = "O carrinho está vazio.")
        List<@NotNull(message = "Item do carrinho inválido.") @Valid ItemPedidoRequest> itens
) {}
