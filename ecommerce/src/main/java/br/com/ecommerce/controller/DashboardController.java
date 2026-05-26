package br.com.ecommerce.controller;

import br.com.ecommerce.model.Pedido;
import br.com.ecommerce.service.CategoriaService;
import br.com.ecommerce.service.ClienteService;
import br.com.ecommerce.service.PedidoService;
import br.com.ecommerce.service.ProdutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.math.BigDecimal;
import java.util.List;

@Controller
public class DashboardController {

    @Autowired
    private CategoriaService categoriaService;

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private ClienteService clienteService;

    @Autowired
    private PedidoService pedidoService;

    @GetMapping("/")
    public String index(Model model) {
        // Estatísticas do painel
        long totalCategorias = categoriaService.listarTodos().size();
        long totalProdutos = produtoService.listarTodos().size();
        long totalClientes = clienteService.listarTodos().size();
        List<Pedido> pedidos = pedidoService.listarTodos();
        long totalPedidos = pedidos.size();

        // Faturamento total
        BigDecimal faturamentoTotal = pedidos.stream()
                .map(Pedido::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Quantidade de produtos com estoque baixo (estoque < 5)
        long estoqueBaixo = produtoService.listarTodos().stream()
                .filter(p -> p.getEstoque() < 5)
                .count();

        model.addAttribute("totalCategorias", totalCategorias);
        model.addAttribute("totalProdutos", totalProdutos);
        model.addAttribute("totalClientes", totalClientes);
        model.addAttribute("totalPedidos", totalPedidos);
        model.addAttribute("faturamentoTotal", faturamentoTotal);
        model.addAttribute("estoqueBaixo", estoqueBaixo);
        model.addAttribute("page", "dashboard"); // Define a página ativa para o menu lateral

        return "dashboard";
    }
}
