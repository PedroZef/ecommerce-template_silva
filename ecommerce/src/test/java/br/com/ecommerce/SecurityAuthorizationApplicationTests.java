package br.com.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.ecommerce.dto.ProdutoRequestDto;
import br.com.ecommerce.ia.tools.EcommerceTools;
import br.com.ecommerce.repository.CategoriaRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SecurityAuthorizationApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final RequestPostProcessor ADMIN = user("admin@admin.com").roles("ADMIN");
	private static final RequestPostProcessor CLIENTE = user("cliente@teste.com").roles("USER");

	@Autowired
	private EcommerceTools ecommerceTools;

	@Autowired
	private CategoriaRepository categoriaRepository;

	private String toJson(Object obj) throws Exception {
		return MAPPER.writeValueAsString(obj);
	}

	// =====================================================================
	// Regras de acesso às rotas (SecurityConfig)
	// =====================================================================

	@Test
	void apiProdutos_LeituraPublicaSemAutenticacao_DeveRetornar200() throws Exception {
		mockMvc.perform(get("/api/produtos"))
			.andExpect(status().isOk());
	}

	@Test
	void apiProdutos_CriacaoSemAutenticacao_DeveSerBloqueada() throws Exception {
		mockMvc.perform(post("/api/produtos")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void apiProdutos_CriacaoPorCliente_DeveRetornar403() throws Exception {
		mockMvc.perform(post("/api/produtos")
				.with(CLIENTE)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void apiProdutos_CriacaoPorAdmin_DeveRetornar201() throws Exception {
		Long categoriaId = categoriaRepository.findByNome("Informática").orElseThrow().getId();
		ProdutoRequestDto dto = new ProdutoRequestDto(
				"Monitor Teste Security", "Monitor da suíte de testes", new BigDecimal("699.00"), 4, categoriaId);

		mockMvc.perform(post("/api/produtos")
				.with(ADMIN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(toJson(dto)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.nome").value("Monitor Teste Security"));
	}

	@Test
	void apiProdutos_PayloadInvalido_DeveRetornar400() throws Exception {
		ProdutoRequestDto dto = new ProdutoRequestDto(null, null, new BigDecimal("-1"), -5, null);

		mockMvc.perform(post("/api/produtos")
				.with(ADMIN)
				.contentType(MediaType.APPLICATION_JSON)
				.content(toJson(dto)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.details").exists());
	}

	@Test
	void apiPedidos_AdminPodeListarTodos() throws Exception {
		mockMvc.perform(get("/api/pedidos").with(ADMIN))
			.andExpect(status().isOk());
	}

	@Test
	void apiPedidos_ClienteNaoPodeListarTodos() throws Exception {
		mockMvc.perform(get("/api/pedidos").with(CLIENTE))
			.andExpect(status().isForbidden());
	}

	@Test
	void paginaCheckout_SemLogin_DeveSerBloqueada() throws Exception {
		mockMvc.perform(get("/checkout"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void paginaProdutos_ProibidaParaCliente() throws Exception {
		mockMvc.perform(get("/produtos").with(CLIENTE))
			.andExpect(status().isForbidden());
	}

	// =====================================================================
	// Autenticação JWT / registro
	// =====================================================================

	@Test
	void registroComSenhaFraca_DeveRetornar400() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"senha.fraca@teste.com\",\"password\":\"123\"}"))
			.andExpect(status().isBadRequest());
	}

	@Test
	void registroComSenhaForte_DeveRetornar200() throws Exception {
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"senha.boa@teste.com\",\"password\":\"Abcdef123@xyz\"}"))
			.andExpect(status().isOk());
	}

	@Test
	void loginComCredenciaisValidas_DeveRetornarTokenJwt() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin@admin.com\",\"password\":\"admin123\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.token").isNotEmpty());
	}

	@Test
	void loginComCredenciaisInvalidas_DeveRetornar401() throws Exception {
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin@admin.com\",\"password\":\"senha-errada\"}"))
			.andExpect(status().isUnauthorized());
	}

	// =====================================================================
	// Autorização das TOOLS do Assistente IA
	// =====================================================================

	@Test
	@WithMockUser(username = "cliente@teste.com", roles = "USER")
	void toolsAdministrativas_ClienteDeveReceberAcessoNegado() {
		assertThat(ecommerceTools.cadastrarProduto("Produto X", "1", BigDecimal.ONE, 1, "CatInexistente"))
			.contains("Acesso negado");
		assertThat(ecommerceTools.obterResumoVendas("justificativa de teste")).contains("Acesso negado");
		assertThat(ecommerceTools.atualizarStatusPedido(1L, "CONCLUIDO")).contains("Acesso negado");
		assertThat(ecommerceTools.cadastrarDespesa("Teste", BigDecimal.ONE, "Outros")).contains("Acesso negado");
		assertThat(ecommerceTools.obterRelatorioDespesas(null)).contains("Acesso negado");
	}

	@Test
	@WithMockUser(username = "cliente@teste.com", roles = "USER")
	void consultasDeCatalogo_ClientePodeUsarNormalmente() {
		assertThat(ecommerceTools.listarProdutosPorCategoria("Roupas")).doesNotContain("Acesso negado");
		assertThat(ecommerceTools.obterEstoqueEPrecoProduto("Smartphone")).doesNotContain("Acesso negado");
	}

	@Test
	@WithMockUser(username = "admin@admin.com", roles = "ADMIN")
	void toolsAdministrativas_AdminPodeExecutar() {
		assertThat(ecommerceTools.obterResumoVendas("teste")).contains("Resumo de Vendas");
		assertThat(ecommerceTools.atualizarStatusPedido(999999L, "CONCLUIDO")).contains("Erro ao atualizar pedido");
		assertThat(ecommerceTools.cadastrarDespesa("Aluguel Teste", new BigDecimal("1500.00"), "Outros"))
			.contains("Sucesso");
	}
}