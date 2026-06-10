package br.com.ecommerce;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import br.com.ecommerce.exception.EstoqueInsuficienteException;
import br.com.ecommerce.model.Categoria;
import br.com.ecommerce.model.Cliente;
import br.com.ecommerce.model.ItemPedido;
import br.com.ecommerce.model.Pedido;
import br.com.ecommerce.model.Produto;
import br.com.ecommerce.repository.CategoriaRepository;
import br.com.ecommerce.repository.ClienteRepository;
import br.com.ecommerce.repository.PedidoRepository;
import br.com.ecommerce.repository.ProdutoRepository;
import br.com.ecommerce.service.PedidoService;

import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev") // Garante que os testes executem sobre o banco H2 em memória
class EcommerceApplicationTests {

	@Autowired
	private PedidoService pedidoService;

	@Autowired
	private CategoriaRepository categoriaRepository;

	@Autowired
	private ProdutoRepository produtoRepository;

	@Autowired
	private ClienteRepository clienteRepository;

	@Autowired
	private PedidoRepository pedidoRepository;

	@Test
	void testContextLoads() {
		// Garante que o contexto do Spring Boot carrega com o perfil dev e banco H2 de
		// forma limpa.
	}

	@Test
	void testCriarPedidoComSucesso_DeveEfetuarCommitEDeduzirEstoque() {
		// 1. Arrange (Preparar)
		Categoria categoria = new Categoria();
		categoria.setNome("Testes Commit");
		categoria = categoriaRepository.save(categoria);

		Produto produto = new Produto();
		produto.setNome("Teclado de Teste");
		produto.setDescricao("Teclado mecânico usado para testar commit");
		produto.setPreco(new BigDecimal("150.00"));
		produto.setEstoque(10); // Estoque original = 10
		produto.setCategoria(categoria);
		produto = produtoRepository.save(produto);

		Cliente cliente = new Cliente();
		cliente.setNome("Comprador de Teste");
		cliente.setEmail("comprador.teste@email.com");
		cliente.setCpf("111.222.333-44");
		cliente = clienteRepository.save(cliente);

		Pedido pedido = new Pedido();
		pedido.setCliente(cliente);

		ItemPedido item = new ItemPedido();
		item.setProduto(produto);
		item.setQuantidade(3); // Compra de 3 unidades
		item.setPrecoUnitario(produto.getPreco());

		pedido.adicionarItem(item);

		// 2. Act (Executar)
		Pedido pedidoSalvo = pedidoService.criarPedido(pedido);

		// 3. Assert (Verificar)
		assertNotNull(pedidoSalvo.getId(), "O pedido deveria ter sido gerado com sucesso (Commit).");
		assertEquals(new BigDecimal("450.00"), pedidoSalvo.getTotal(), "O valor total deveria ser R$ 450.00.");

		// Busca o produto novamente no banco e valida que o estoque foi decrementado
		// para 7
		Optional<Produto> produtoAtualizado = produtoRepository.findById(produto.getId());
		assertTrue(produtoAtualizado.isPresent());
		assertEquals(7, produtoAtualizado.get().getEstoque(), "O estoque do produto deveria ter sido reduzido para 7.");
	}

	@Test
	void testCriarPedidoComEstoqueInsuficiente_DeveLancarExcecaoEEfetuarRollback() {
		// 1. Arrange (Preparar)
		Categoria categoria = new Categoria();
		categoria.setNome("Testes Rollback");
		categoria = categoriaRepository.save(categoria);

		Produto produto = new Produto();
		produto.setNome("Mouse de Teste");
		produto.setDescricao("Mouse óptico usado para testar rollback");
		produto.setPreco(new BigDecimal("50.00"));
		produto.setEstoque(10); // Estoque original = 10
		produto.setCategoria(categoria);
		produto = produtoRepository.save(produto);

		Cliente cliente = new Cliente();
		cliente.setNome("Comprador de Teste 2");
		cliente.setEmail("comprador.teste2@email.com");
		cliente.setCpf("555.666.777-88");
		cliente = clienteRepository.save(cliente);

		// Cria um pedido que excede o estoque disponível (solicitando 12 unidades)
		Pedido pedido = new Pedido();
		pedido.setCliente(cliente);

		ItemPedido item = new ItemPedido();
		item.setProduto(produto);
		item.setQuantidade(12); // Quantidade solicitada = 12, estoque = 10
		item.setPrecoUnitario(produto.getPreco());

		pedido.adicionarItem(item);

		// Obtém a quantidade total de pedidos cadastrados no sistema antes da tentativa
		long totalPedidosAntes = pedidoRepository.count();

		// 2. Act & 3. Assert (Executar e Verificar)
		assertThrows(EstoqueInsuficienteException.class, () -> {
			pedidoService.criarPedido(pedido);
		}, "Deveria ter lançado EstoqueInsuficienteException devido à falta de estoque.");

		// Busca o produto novamente no banco de dados para provar o ROLLBACK!
		Optional<Produto> produtoNaoAlterado = produtoRepository.findById(produto.getId());
		assertTrue(produtoNaoAlterado.isPresent());
		assertEquals(10, produtoNaoAlterado.get().getEstoque(),
				"O estoque do produto deve continuar sendo 10 devido ao Rollback da transação!");

		// Valida que nenhum novo registro de pedido foi gravado na tabela
		long totalPedidosDepois = pedidoRepository.count();
		assertEquals(totalPedidosAntes, totalPedidosAfter(totalPedidosDepois),
				"A tabela de pedidos não deve conter novos registros.");
	}

	private long totalPedidosAfter(long totalPedidosDepois) {
		return totalPedidosDepois;
	}
}