package br.com.gamestore.pi_3.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regressão do Problema 1: compra concluída => pedido/jogo visível para o cliente (e só para ele). */
class CompraVisivelParaClienteIntegrationTest extends IntegracaoBase {

    private JsonNode meusPedidos(Sessao s) throws Exception {
        String corpo = mvc.perform(get("/api/pedidos/meus").header("Authorization", "Bearer " + s.token()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo);
    }

    @Test
    void compraConcluidaApareceEmMeusPedidos() throws Exception {
        Sessao ana = registrar("ana");

        String resposta = mvc.perform(compra(ana, UUID.randomUUID().toString(), 2))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long pedidoId = json.readTree(resposta).get("id").asLong();

        JsonNode lista = meusPedidos(ana);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).get("id").asLong()).isEqualTo(pedidoId);
        assertThat(lista.get(0).get("status").asText()).isEqualTo("CONCLUIDA");
        assertThat(lista.get(0).get("itens")).hasSize(1);
        assertThat(lista.get(0).get("itens").get(0).get("titulo").asText()).isEqualTo(jogoTitulo);
        assertThat(lista.get(0).get("itens").get(0).get("quantidade").asInt()).isEqualTo(2);
    }

    @Test
    void repetirOPostComAMesmaChaveNaoDuplicaOPedido() throws Exception {
        Sessao bia = registrar("bia");
        String chave = UUID.randomUUID().toString();

        String primeira = mvc.perform(compra(bia, chave, 1))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String segunda = mvc.perform(compra(bia, chave, 1))
                .andExpect(status().isOk()) // 200: devolveu o pedido que já existia
                .andReturn().getResponse().getContentAsString();

        assertThat(json.readTree(segunda).get("id").asLong()).isEqualTo(json.readTree(primeira).get("id").asLong());
        assertThat(meusPedidos(bia)).hasSize(1);
    }

    @Test
    void chavesDiferentesCriamPedidosDiferentes() throws Exception {
        Sessao caio = registrar("caio");

        mvc.perform(compra(caio, UUID.randomUUID().toString(), 1)).andExpect(status().isCreated());
        mvc.perform(compra(caio, UUID.randomUUID().toString(), 1)).andExpect(status().isCreated());

        assertThat(meusPedidos(caio)).hasSize(2);
    }

    @Test
    void clienteNaoVePedidosDeOutroCliente() throws Exception {
        Sessao dani = registrar("dani");
        Sessao edu = registrar("edu");
        mvc.perform(compra(dani, UUID.randomUUID().toString(), 1)).andExpect(status().isCreated());

        assertThat(meusPedidos(dani)).hasSize(1);
        assertThat(meusPedidos(edu)).isEmpty();
    }

    @Test
    void semTokenRetorna401() throws Exception {
        mvc.perform(get("/api/pedidos/meus")).andExpect(status().isUnauthorized());
    }

    @Test
    void chaveDeIdempotenciaInvalidaRetorna400() throws Exception {
        Sessao fabi = registrar("fabi");

        mvc.perform(compra(fabi, "curta", 1)).andExpect(status().isBadRequest());
        assertThat(meusPedidos(fabi)).isEmpty();
    }
}
