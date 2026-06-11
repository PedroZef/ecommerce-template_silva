package br.com.ecommerce.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.com.ecommerce.exception.EstoqueInsuficienteException;
import br.com.ecommerce.model.Cliente;
import br.com.ecommerce.model.ItemPedido;
import br.com.ecommerce.model.MeioPagamento;
import br.com.ecommerce.model.Pedido;
import br.com.ecommerce.model.Produto;
import br.com.ecommerce.service.ClienteService;
import br.com.ecommerce.service.PedidoService;
import br.com.ecommerce.service.ProdutoService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;

    @GetMapping("/pedidos")
    public String listar(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            model.addAttribute("pedidos", pedidoService.listarTodos());
        } else {
            // Clientes normais só podem ver seu próprio histórico de pedidos
            Cliente loggedCliente = clienteService.buscarPorEmail(auth.getName()).orElse(null);
            if (loggedCliente != null) {
                // Filtra ou apenas exibe do cliente logado.
                // Para manter simples, podemos filtrar a lista de todos os pedidos no
                // controlador
                List<Pedido> pedidosFiltrados = pedidoService.listarTodos().stream()
                        .filter(p -> p.getCliente().getId().equals(loggedCliente.getId()))
                        .toList();
                model.addAttribute("pedidos", pedidosFiltrados);
            } else {
                model.addAttribute("pedidos", new ArrayList<Pedido>());
            }
        }
        model.addAttribute("page", "pedidos");
        return "pedidos";
    }

    @GetMapping("/checkout")
    public String exibirCheckout(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            model.addAttribute("clientes", clienteService.listarTodos());
        } else {
            Cliente loggedCliente = clienteService.buscarPorEmail(auth.getName()).orElse(null);
            model.addAttribute("loggedCliente", loggedCliente);
            if (loggedCliente != null) {
                model.addAttribute("clienteId", loggedCliente.getId());
            }
        }

        model.addAttribute("produtos", produtoService.listarTodos());
        model.addAttribute("page", "checkout");
        return "checkout";
    }

    @PostMapping("/checkout/comprar")
    public String processarCheckout(
            @RequestParam(value = "clienteId", required = false) Long clienteId,
            @RequestParam(value = "produtoId", required = false) List<Long> produtoIds,
            @RequestParam(value = "quantidade", required = false) List<Integer> quantidades,
            @RequestParam("meioPagamento") MeioPagamento meioPagamento,
            @RequestParam(value = "numCartao", required = false) String numCartao,
            @RequestParam(value = "nomeCartao", required = false) String nomeCartao,
            @RequestParam(value = "validadeCartao", required = false) String validadeCartao,
            @RequestParam(value = "cvvCartao", required = false) String cvvCartao,
            @RequestParam(value = "parcelas", required = false) Integer parcelas,
            RedirectAttributes redirectAttributes) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Se não for Admin, força o clienteId a ser o do usuário autenticado por
        // segurança
        final Long finalClienteId;
        if (!isAdmin) {
            Cliente loggedCliente = clienteService.buscarPorEmail(auth.getName())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Cliente associado ao usuário logado não foi encontrado."));
            finalClienteId = loggedCliente.getId();
        } else {
            if (clienteId == null) {
                redirectAttributes.addFlashAttribute("error", "Selecione um cliente para finalizar o checkout.");
                return "redirect:/checkout";
            }
            finalClienteId = clienteId;
        }

        if (produtoIds == null || quantidades == null || produtoIds.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Selecione pelo menos um produto para realizar a compra.");
            return "redirect:/checkout";
        }

        try {
            // 1. Busca o cliente
            Cliente cliente = clienteService.buscarPorId(finalClienteId)
                    .orElseThrow(
                            () -> new IllegalArgumentException("Cliente não encontrado com o ID: " + finalClienteId));

            // 2. Instancia o Pedido
            Pedido pedido = new Pedido();
            pedido.setCliente(cliente);
            pedido.setMeioPagamento(meioPagamento);
            switch (meioPagamento) {
                case CARTAO_CREDITO -> {
                    pedido.setParcelas(parcelas != null ? parcelas : 1);
                    String cleanNum = numCartao != null ? numCartao.replaceAll("\\D", "") : "";
                    String last4 = cleanNum.length() >= 4 ? cleanNum.substring(cleanNum.length() - 4) : "xxxx";
                    pedido.setDetalhesPagamento(String.format("Crédito final %s - %dx", last4, pedido.getParcelas()));
                }
                case CARTAO_DEBITO -> {
                    pedido.setParcelas(null);
                    String cleanNum = numCartao != null ? numCartao.replaceAll("\\D", "") : "";
                    String last4 = cleanNum.length() >= 4 ? cleanNum.substring(cleanNum.length() - 4) : "xxxx";
                    pedido.setDetalhesPagamento(String.format("Débito final %s", last4));
                }
                case PIX -> {
                    pedido.setParcelas(null);
                    pedido.setDetalhesPagamento("Pix Simulado");
                }
                case BOLETO -> {
                    pedido.setParcelas(null);
                    pedido.setDetalhesPagamento("Boleto Bancário (3 dias úteis)");
                }
            }

            // 3. Monta os itens do pedido
            for (int i = 0; i < produtoIds.size(); i++) {
                Long prodId = produtoIds.get(i);
                Integer qtd = quantidades.get(i);

                // Ignora produtos que foram submetidos com quantidade nula, vazia ou menor que
                // 1
                if (qtd == null || qtd <= 0) {
                    continue;
                }

                Produto produto = produtoService.buscarPorId(prodId)
                        .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado com o ID: " + prodId));

                ItemPedido item = new ItemPedido();
                item.setProduto(produto);
                item.setQuantidade(qtd);
                item.setPrecoUnitario(produto.getPreco()); // Preenche provisoriamente, o service recalcula formalmente

                pedido.adicionarItem(item);
            }

            if (pedido.getItens().isEmpty()) {
                redirectAttributes.addFlashAttribute("error",
                        "Por favor, selecione pelo menos um produto com quantidade maior que zero.");
                return "redirect:/checkout";
            }

            // 4. Invoca o serviço transacional
            Pedido pedidoSalvo = pedidoService.criarPedido(pedido);

            // Transação bem sucedida -> COMMIT!
            redirectAttributes.addFlashAttribute("success",
                    String.format(
                            "Transação Realizada com Sucesso (Commit)! O pedido #%d no valor total de R$ %,.2f foi gerado e os estoques dos produtos foram deduzidos.",
                            pedidoSalvo.getId(), pedidoSalvo.getTotal()));

            return "redirect:/pedidos";

        } catch (EstoqueInsuficienteException e) {
            // Transação falhou devido à lógica de negócio -> ROLLBACK automático!
            redirectAttributes.addFlashAttribute("acidError", e.getMessage());
            redirectAttributes.addFlashAttribute("selectedClienteId", clienteId);
            return "redirect:/checkout";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao processar a compra: " + e.getMessage());
            return "redirect:/checkout";
        }
    }
}
