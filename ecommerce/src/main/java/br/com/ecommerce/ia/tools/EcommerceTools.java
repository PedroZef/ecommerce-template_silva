package br.com.ecommerce.ia.tools;

import br.com.ecommerce.model.*;
import br.com.ecommerce.repository.*;
import br.com.ecommerce.service.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class EcommerceTools {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final PedidoRepository pedidoRepository;
    private final PedidoService pedidoService;

    public EcommerceTools(ProdutoRepository produtoRepository,
                          CategoriaRepository categoriaRepository,
                          PedidoRepository pedidoRepository,
                          PedidoService pedidoService) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.pedidoRepository = pedidoRepository;
        this.pedidoService = pedidoService;
    }

    private String getUsuarioLogado() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "anonymousUser";
    }

    @Tool(description = "Registra um novo produto no e-commerce. Se a categoria não existir, ela será criada automaticamente.")
    public String cadastrarProduto(String nome, String descricao, BigDecimal preco, Integer estoque, String nomeCategoria) {
        String usuario = getUsuarioLogado();
        
        Optional<Categoria> categoriaOpt = categoriaRepository.findByNome(nomeCategoria);
        Categoria categoria;
        if (categoriaOpt.isEmpty()) {
            categoria = new Categoria();
            categoria.setNome(nomeCategoria);
            categoria = categoriaRepository.save(categoria);
        } else {
            categoria = categoriaOpt.get();
        }

        Produto produto = new Produto();
        produto.setNome(nome);
        produto.setDescricao(descricao);
        produto.setPreco(preco);
        produto.setEstoque(estoque);
        produto.setCategoria(categoria);

        Produto salvo = produtoRepository.save(produto);
        return String.format("Sucesso: Produto '%s' cadastrado com ID %d na categoria '%s' por '%s'. Preço: R$ %.2f, Estoque: %d.",
                salvo.getNome(), salvo.getId(), categoria.getNome(), usuario, salvo.getPreco(), salvo.getEstoque());
    }

    @Tool(description = "Busca detalhes de um produto pelo nome (preço, estoque, descrição e categoria).")
    public String obterEstoqueEPrecoProduto(String nomeProduto) {
        List<Produto> produtos = produtoRepository.findAll().stream()
                .filter(p -> p.getNome().toLowerCase().contains(nomeProduto.toLowerCase()))
                .collect(Collectors.toList());

        if (produtos.isEmpty()) {
            return "Nenhum produto encontrado com o nome '" + nomeProduto + "'.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Produtos encontrados (").append(produtos.size()).append("):\n");
        for (Produto p : produtos) {
            sb.append(String.format("- %s (ID: %d): Preço R$ %.2f | Estoque: %d | Categoria: %s | Descrição: %s\n",
                    p.getNome(), p.getId(), p.getPreco(), p.getEstoque(), p.getCategoria().getNome(), p.getDescricao()));
        }
        return sb.toString();
    }

    @Tool(description = "Lista todos os produtos de uma determinada categoria.")
    public String listarProdutosPorCategoria(String nomeCategoria) {
        Optional<Categoria> categoriaOpt = categoriaRepository.findByNome(nomeCategoria);
        if (categoriaOpt.isEmpty()) {
            return "Categoria '" + nomeCategoria + "' não encontrada no sistema.";
        }
        Categoria cat = categoriaOpt.get();
        List<Produto> produtos = produtoRepository.findByCategoriaId(cat.getId());
        if (produtos.isEmpty()) {
            return "Nenhum produto cadastrado na categoria '" + nomeCategoria + "'.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Produtos na categoria '").append(nomeCategoria).append("':\n");
        for (Produto p : produtos) {
            sb.append(String.format("- %s: R$ %.2f (Estoque: %d)\n", p.getNome(), p.getPreco(), p.getEstoque()));
        }
        return sb.toString();
    }

    @Tool(description = "Retorna um resumo geral de vendas, faturamento total e informações sobre os últimos 5 pedidos do e-commerce. Parâmetro 'justificativa' deve ser uma explicação curta.")
    public String obterResumoVendas(String justificativa) {
        long totalPedidos = pedidoService.contarTodos();
        BigDecimal faturamento = pedidoService.calcularFaturamentoTotal();
        List<Pedido> pedidos = pedidoService.listarTodos();

        StringBuilder sb = new StringBuilder();
        sb.append("Resumo de Vendas (Consulta autorizada para: ").append(getUsuarioLogado()).append("):\n")
          .append("- Total de Pedidos: ").append(totalPedidos).append("\n")
          .append("- Faturamento Total: R$ ").append(String.format("%.2f", faturamento)).append("\n")
          .append("Últimos pedidos registrados:\n");

        List<Pedido> ultimosPedidos = pedidos.stream()
                .sorted((p1, p2) -> p2.getId().compareTo(p1.getId()))
                .limit(5)
                .collect(Collectors.toList());

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        for (Pedido p : ultimosPedidos) {
            sb.append(String.format("* Pedido #%d: R$ %.2f | Cliente: %s | Data: %s | Status: %s\n",
                    p.getId(), p.getTotal(), p.getCliente().getNome(), p.getDataPedido().format(dtf), p.getStatus()));
        }

        return sb.toString();
    }

    @Tool(description = "Atualiza o status de um pedido específico. Parâmetros: idPedido (Long) e novoStatus (PENDENTE, CONCLUIDO ou CANCELADO).")
    public String atualizarStatusPedido(Long idPedido, String novoStatus) {
        Optional<Pedido> pedidoOpt = pedidoService.buscarPorId(idPedido);
        if (pedidoOpt.isEmpty()) {
            return "Pedido de ID " + idPedido + " não foi encontrado.";
        }

        Pedido pedido = pedidoOpt.get();
        OrderStatus statusAntigo = pedido.getStatus();
        try {
            OrderStatus statusNovo = OrderStatus.valueOf(novoStatus.toUpperCase().trim());
            pedido.setStatus(statusNovo);
            pedidoRepository.save(pedido);
            return String.format("Pedido #%d atualizado com sucesso de '%s' para '%s'.",
                    idPedido, statusAntigo, statusNovo);
        } catch (IllegalArgumentException e) {
            return String.format("Erro: Status '%s' é inválido. Escolha um dos seguintes: PENDENTE, CONCLUIDO, CANCELADO.", novoStatus);
        }
    }
}
