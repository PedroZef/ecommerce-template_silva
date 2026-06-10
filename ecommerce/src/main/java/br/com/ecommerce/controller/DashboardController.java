package br.com.ecommerce.controller;

import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import br.com.ecommerce.service.CategoriaService;
import br.com.ecommerce.service.ClienteService;
import br.com.ecommerce.service.PedidoService;
import br.com.ecommerce.service.ProdutoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final CategoriaService categoriaService;
    private final ProdutoService produtoService;
    private final ClienteService clienteService;
    private final PedidoService pedidoService;

    @GetMapping("/")
    public String index(HttpServletRequest request, Model model) {
        if (!request.isUserInRole("ROLE_ADMIN")) {
            return "redirect:/checkout";
        }

        // Estatísticas do painel usando métodos otimizados no banco de dados
        long totalCategorias = categoriaService.contarTodas();
        long totalProdutos = produtoService.contarTodos();
        long totalClientes = clienteService.contarTodos();
        long totalPedidos = pedidoService.contarTodos();

        // Faturamento total calculado via SUM no banco
        BigDecimal faturamentoTotal = pedidoService.calcularFaturamentoTotal();

        // Quantidade de produtos com estoque baixo (estoque < 5) calculada no banco
        long estoqueBaixo = produtoService.contarEstoqueBaixo(5);

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
