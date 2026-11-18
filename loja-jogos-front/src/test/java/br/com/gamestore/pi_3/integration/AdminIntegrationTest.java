package br.com.gamestore.pi_3.integration;

import br.com.gamestore.pi_3.model.AuditoriaAdmin;
import br.com.gamestore.pi_3.model.Pedido;
import br.com.gamestore.pi_3.model.Usuario;
import br.com.gamestore.pi_3.repository.AuditoriaAdminRepository;
import br.com.gamestore.pi_3.repository.PedidoRepository;
import br.com.gamestore.pi_3.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Exclusão de clientes e dashboard, passando pelo AuthInterceptor de verdade (401/403/200). */
class AdminIntegrationTest extends IntegracaoBase {

    @Autowired PedidoRepository pedidos;
    @Autowired UsuarioRepository usuarios;
    @Autowired AuditoriaAdminRepository auditoria;

    // ---------- Excluir clientes ----------

    @Test
    void semTokenRetorna401EClienteComumRecebe403() throws Exception {
        Sessao alvo = registrar("alvo");
        Sessao intruso = registrar("intruso");

        mvc.perform(delete("/api/admin/usuarios/" + alvo.id())).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/admin/usuarios/" + alvo.id()).header("Authorization", "Bearer " + intruso.token()))
                .andExpect(status().isForbidden());

        assertThat(usuarios.findById(alvo.id())).isPresent();
    }

    @Test
    void adminExcluiClienteMantemPedidosERegistraAuditoria() throws Exception {
        Sessao cliente = registrar("cliente");
        mvc.perform(compra(cliente, UUID.randomUUID().toString(), 1)).andExpect(status().isCreated());

        String resposta = mvc.perform(delete("/api/admin/usuarios/" + cliente.id())
                        .header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(resposta).get("pedidosPreservados").asInt()).isEqualTo(1);

        assertThat(usuarios.findById(cliente.id())).isEmpty();

        List<Pedido> preservados = pedidos.findAll().stream()
                .filter(p -> cliente.email().equals(p.getClienteEmail())).toList();
        assertThat(preservados).hasSize(1);
        assertThat(preservados.get(0).getUsuario()).isNull();

        List<AuditoriaAdmin> trilha = auditoria.findAll().stream()
                .filter(a -> cliente.id().equals(a.getAlvoId())).toList();
        assertThat(trilha).hasSize(1);
        assertThat(trilha.get(0).getAcao()).isEqualTo(AuditoriaAdmin.EXCLUSAO_USUARIO);
        assertThat(trilha.get(0).getAdminEmail()).isEqualTo("admin");
        assertThat(trilha.get(0).getAlvoEmail()).isEqualTo(cliente.email());
        assertThat(trilha.get(0).getDataHora()).isNotNull();

        // a sessão do cliente excluído deixa de valer imediatamente
        mvc.perform(get("/api/pedidos/meus").header("Authorization", "Bearer " + cliente.token()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void naoExcluiOutroAdminNemUsuarioInexistente() throws Exception {
        Usuario outroAdmin = usuarios.save(new Usuario(null, "Outro Admin",
                "admin2-" + UUID.randomUUID() + "@teste.com", "x", "ROLE_ADMIN"));
        String admin = "Bearer " + tokenAdmin();

        mvc.perform(delete("/api/admin/usuarios/" + outroAdmin.getId()).header("Authorization", admin))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/admin/usuarios/99999999").header("Authorization", admin))
                .andExpect(status().isNotFound());

        assertThat(usuarios.findById(outroAdmin.getId())).isPresent();
    }

    // ---------- Dashboard ----------

    @Test
    void dashboardExigeAdmin() throws Exception {
        Sessao cliente = registrar("curioso");

        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/dashboard").header("Authorization", "Bearer " + cliente.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardConsideraAsComprasRecentes() throws Exception {
        Sessao cliente = registrar("comprador");
        mvc.perform(compra(cliente, UUID.randomUUID().toString(), 3)).andExpect(status().isCreated());

        String corpo = mvc.perform(get("/api/admin/dashboard?top=3").header("Authorization", "Bearer " + tokenAdmin()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode d = json.readTree(corpo);

        assertThat(d.get("kpis").get("totalPedidos").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(d.get("kpis").get("totalPedidosGeral").asLong()).isGreaterThanOrEqualTo(d.get("kpis").get("totalPedidos").asLong());
        assertThat(d.get("kpis").get("faturamento").decimalValue().signum()).isPositive();
        assertThat(d.get("evolucao")).hasSize(30); // padrão: últimos 30 dias, com dias vazios preenchidos
        assertThat(d.get("porStatus").get(0).get("status").asText()).isEqualTo("CONCLUIDA");
        assertThat(d.get("topJogos").size()).isBetween(1, 3);
    }

    @Test
    void dashboardRejeitaParametrosInvalidos() throws Exception {
        String admin = "Bearer " + tokenAdmin();

        mvc.perform(get("/api/admin/dashboard?inicio=2026-11-10&fim=2026-11-01").header("Authorization", admin))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/dashboard?top=0").header("Authorization", admin))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/dashboard?inicio=2024-01-01&fim=2026-01-01&agrupamento=DIA").header("Authorization", admin))
                .andExpect(status().isBadRequest());
    }
}
