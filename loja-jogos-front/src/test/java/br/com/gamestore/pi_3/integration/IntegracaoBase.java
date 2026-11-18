package br.com.gamestore.pi_3.integration;

import br.com.gamestore.pi_3.repository.JogoRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a aplicação inteira (controllers + interceptor + JPA) contra um H2 EM MEMÓRIA, para nunca encostar em
 * ./data/gamestoredb. O DataInitializer roda e cria o admin (admin/admin123) e os jogos de exemplo.
 * Os testes não limpam o banco: cada um usa e-mails únicos e só afirma sobre os dados que ele mesmo criou.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:lojatest;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.upload.dir=./target/test-assets/jogos"
})
@AutoConfigureMockMvc
abstract class IntegracaoBase {

    protected record Sessao(Long id, String email, String token) {}

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected JogoRepository jogos;

    protected long jogoId;
    protected String jogoTitulo;

    @BeforeEach
    void escolherJogo() {
        var jogo = jogos.findAll().get(0);
        jogoId = jogo.getId();
        jogoTitulo = jogo.getTitulo();
    }

    /** Cadastra um cliente novo. O cadastro já deve devolver um token utilizável (era um dos bugs). */
    protected Sessao registrar(String prefixo) throws Exception {
        String email = prefixo + "-" + UUID.randomUUID() + "@teste.com";
        String corpo = json.writeValueAsString(Map.of("nome", "Cliente Teste", "email", email, "senha", "segredo123"));
        MvcResult r = mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode n = json.readTree(r.getResponse().getContentAsString());
        String token = n.path("token").asText("");
        if (token.isBlank()) throw new AssertionError("O cadastro deveria devolver um token de sessão.");
        return new Sessao(n.get("id").asLong(), email, token);
    }

    protected String tokenAdmin() throws Exception {
        String corpo = json.writeValueAsString(Map.of("username", "admin", "password", "admin123"));
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(r.getResponse().getContentAsString()).get("token").asText();
    }

    /** POST /api/pedidos com 'quantidade' unidades do jogo escolhido. chave == null -> sem Idempotency-Key. */
    protected MockHttpServletRequestBuilder compra(Sessao s, String chave, int quantidade) throws Exception {
        String corpo = json.writeValueAsString(Map.of("itens",
                java.util.List.of(Map.of("jogoId", jogoId, "quantidade", quantidade))));
        MockHttpServletRequestBuilder req = post("/api/pedidos")
                .header("Authorization", "Bearer " + s.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
        return chave == null ? req : req.header("Idempotency-Key", chave);
    }
}
