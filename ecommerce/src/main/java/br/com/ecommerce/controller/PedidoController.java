package br.com.ecommerce.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.ecommerce.exception.EstoqueInsuficienteException;
import br.com.ecommerce.model.Cliente;
import br.com.ecommerce.model.MeioPagamento;
import br.com.ecommerce.model.OrderStatus;
import br.com.ecommerce.model.Pedido;
import br.com.ecommerce.service.CheckoutService;
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
    private final CheckoutService checkoutService;

    @GetMapping("/pedidos")
    public String listar(Model model,
                         @RequestParam(value = "success", required = false) String success,
                         @RequestParam(value = "error", required = false) String error) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            model.addAttribute("pedidos", pedidoService.listarTodos());
        } else {
            Cliente loggedCliente = clienteService.buscarPorEmail(auth.getName()).orElse(null);
            if (loggedCliente != null) {
                model.addAttribute("pedidos", pedidoService.listarPorClienteId(loggedCliente.getId()));
            } else {
                model.addAttribute("pedidos", new ArrayList<Pedido>());
            }
        }
        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);
        model.addAttribute("page", "pedidos");
        return "pedidos";
    }

    @GetMapping("/checkout")
    public String exibirCheckout(Model model,
                                 @RequestParam(value = "error", required = false) String error,
                                 @RequestParam(value = "acidError", required = false) String acidError,
                                 @RequestParam(value = "selectedClienteId", required = false) Long selectedClienteId) {
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

        if (error != null) model.addAttribute("error", error);
        if (acidError != null) model.addAttribute("acidError", acidError);
        if (selectedClienteId != null) model.addAttribute("selectedClienteId", selectedClienteId);

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
            @RequestParam(value = "parcelas", required = false) Integer parcelas) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        final Long finalClienteId;
        if (!isAdmin) {
            Cliente loggedCliente = clienteService.buscarPorEmail(auth.getName())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Cliente associado ao usuário logado não foi encontrado."));
            finalClienteId = loggedCliente.getId();
        } else {
            if (clienteId == null) {
                return "redirect:/checkout?error=Selecione+um+cliente+para+finalizar+o+checkout.";
            }
            finalClienteId = clienteId;
        }

        try {
            Pedido pedidoSalvo = checkoutService.processarCheckout(
                    finalClienteId, produtoIds, quantidades, meioPagamento,
                    parcelas, numCartao, nomeCartao, validadeCartao, cvvCartao
            );

            String msg = URLEncoder.encode(
                    String.format(
                            "Transação Realizada com Sucesso (Commit)! O pedido #%d no valor total de R$ %,.2f foi gerado e os estoques dos produtos foram deduzidos.",
                            pedidoSalvo.getId(), pedidoSalvo.getTotal()),
                    StandardCharsets.UTF_8);

            return "redirect:/pedidos?success=" + msg;

        } catch (EstoqueInsuficienteException e) {
            String err = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            String selCliente = clienteId != null ? "&selectedClienteId=" + clienteId : "";
            return "redirect:/checkout?acidError=" + err + selCliente;
        } catch (Exception e) {
            String err = URLEncoder.encode("Erro ao processar a compra: " + e.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/checkout?error=" + err;
        }
    }

    @PostMapping("/pedidos/atualizar-status")
    public String atualizarStatus(
            @RequestParam("id") Long id,
            @RequestParam("status") OrderStatus status) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            return "redirect:/pedidos?error=Apenas+administradores+podem+alterar+o+status+de+um+pedido.";
        }

        try {
            pedidoService.atualizarStatus(id, status);
            String msg = URLEncoder.encode(
                    "Status do pedido #" + id + " atualizado para " + status + " com sucesso.",
                    StandardCharsets.UTF_8);
            return "redirect:/pedidos?success=" + msg;
        } catch (Exception e) {
            String err = URLEncoder.encode("Erro ao atualizar status do pedido: " + e.getMessage(), StandardCharsets.UTF_8);
            return "redirect:/pedidos?error=" + err;
        }
    }
}