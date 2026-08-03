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
    private final PedidoService pedidoService;
    private final DespesaRepository despesaRepository;

    public EcommerceTools(ProdutoRepository produtoRepository,
                          CategoriaRepository categoriaRepository,
                          PedidoService pedidoService,
                          DespesaRepository despesaRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.pedidoService = pedidoService;
        this.despesaRepository = despesaRepository;
    }

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private String getUsuarioLogado() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "anonymousUser";
    }

    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(ROLE_ADMIN));
    }

    private String negarAcesso(String acao) {
        return "Acesso negado: a ação '" + acao
                + "' exige privilégios de administrador (ROLE_ADMIN). Seu usuário é '" + getUsuarioLogado()
                + "'. Consulte um administrador ou solicite a ele que execute a operação.";
    }

    @Tool(description = "Registra um novo produto no e-commerce. Se a categoria não existir, ela será criada automaticamente. Apenas administradores (ROLE_ADMIN) podem executar.")
    public String cadastrarProduto(String nome, String descricao, BigDecimal preco, Integer estoque, String nomeCategoria) {
        if (!isAdmin()) {
            return negarAcesso("Cadastrar Produto");
        }

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

    @Tool(description = "Busca detalhes de um produto pelo nome (preço, estoque, descrição e categoria). Disponível para qualquer usuário autenticado.")
    public String obterEstoqueEPrecoProduto(String nomeProduto) {
        List<Produto> produtos = produtoRepository.findByNomeContainingIgnoreCase(nomeProduto);

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

    @Tool(description = "Lista todos os produtos de uma determinada categoria. Disponível para qualquer usuário autenticado.")
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

    @Tool(description = "Retorna um resumo geral de vendas, faturamento total e informações sobre os últimos 5 pedidos do e-commerce. Parâmetro 'justificativa' deve ser uma explicação curta. Apenas administradores (ROLE_ADMIN) podem executar.")
    public String obterResumoVendas(String justificativa) {
        if (!isAdmin()) {
            return negarAcesso("Resumo de Vendas / Faturamento");
        }

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

    @Tool(description = "Atualiza o status de um pedido específico. Parâmetros: idPedido (Long) e novoStatus (PENDENTE, CONCLUIDO ou CANCELADO). Apenas administradores (ROLE_ADMIN) podem executar.")
    public String atualizarStatusPedido(Long idPedido, String novoStatus) {
        if (!isAdmin()) {
            return negarAcesso("Atualizar Status do Pedido");
        }

        try {
            OrderStatus statusNovo = OrderStatus.valueOf(novoStatus.toUpperCase().trim());
            Pedido pedido = pedidoService.atualizarStatus(idPedido, statusNovo);
            return String.format("Pedido #%d atualizado com sucesso para '%s'.", idPedido, pedido.getStatus());
        } catch (IllegalArgumentException e) {
            return String.format("Erro: Status '%s' é inválido. Escolha um dos seguintes: PENDENTE, CONCLUIDO, CANCELADO.", novoStatus);
        } catch (Exception e) {
            return "Erro ao atualizar pedido #" + idPedido + ": " + e.getMessage();
        }
    }

    @Tool(description = "Registra um novo gasto pessoal ou despesa. Categorias recomendadas: Supermercado, Farmácia, Outros. Apenas administradores (ROLE_ADMIN) podem executar.")
    public String cadastrarDespesa(String descricao, BigDecimal valor, String categoria) {
        if (!isAdmin()) {
            return negarAcesso("Cadastrar Despesa");
        }

        Despesa despesa = new Despesa(descricao, valor, categoria.trim());
        Despesa salva = despesaRepository.save(despesa);
        return String.format("Sucesso: Despesa '%s' de R$ %.2f cadastrada na categoria '%s'. (ID: %d)",
                salva.getDescricao(), salva.getValor(), salva.getCategoria(), salva.getId());
    }

    @Tool(description = "Lista todos os gastos cadastrados, com opção de filtrar por categoria (Supermercado, Farmácia, etc.) e mostra o total acumulado. Apenas administradores (ROLE_ADMIN) podem executar.")
    public String obterRelatorioDespesas(String categoriaFiltro) {
        if (!isAdmin()) {
            return negarAcesso("Relatório de Despesas");
        }

        List<Despesa> despesas;
        if (categoriaFiltro != null && !categoriaFiltro.trim().isEmpty() && !categoriaFiltro.equalsIgnoreCase("todas")) {
            despesas = despesaRepository.findByCategoriaIgnoreCase(categoriaFiltro.trim());
        } else {
            despesas = despesaRepository.findAll();
        }

        if (despesas.isEmpty()) {
            return "Nenhum gasto ou despesa foi encontrado para o filtro solicitado.";
        }

        BigDecimal total = despesas.stream()
                .map(Despesa::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder();
        sb.append("=== Relatório de Despesas ===\n");
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        for (Despesa d : despesas) {
            sb.append(String.format("- %s (Cat: %s): R$ %.2f | Data: %s\n",
                    d.getDescricao(), d.getCategoria(), d.getValor(), d.getDataDespesa().format(dtf)));
        }
        sb.append(String.format("Total Acumulado: R$ %.2f", total));
        return sb.toString();
    }
}
