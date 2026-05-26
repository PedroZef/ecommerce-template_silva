package br.com.ecommerce.controller;

import br.com.ecommerce.model.Cliente;
import br.com.ecommerce.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService service;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", service.listarTodos());
        if (!model.containsAttribute("cliente")) {
            model.addAttribute("cliente", new Cliente());
        }
        model.addAttribute("page", "clientes");
        return "clientes";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("cliente") Cliente cliente,
                         BindingResult result,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.cliente", result);
            redirectAttributes.addFlashAttribute("cliente", cliente);
            redirectAttributes.addFlashAttribute("error", "Erro ao salvar cliente. Verifique as validações dos campos.");
            return "redirect:/clientes";
        }

        try {
            service.salvar(cliente);
            redirectAttributes.addFlashAttribute("success", "Cliente '" + cliente.getNome() + "' cadastrado com sucesso!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.cliente", result);
            redirectAttributes.addFlashAttribute("cliente", cliente);
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao salvar cliente: " + e.getMessage());
        }
        return "redirect:/clientes";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Cliente cliente = service.buscarPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado com o ID: " + id));
            model.addAttribute("clientes", service.listarTodos());
            model.addAttribute("cliente", cliente);
            model.addAttribute("page", "clientes");
            return "clientes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/clientes";
        }
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            service.excluir(id);
            redirectAttributes.addFlashAttribute("success", "Cliente excluído com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Este cliente não pode ser excluído porque possui histórico de pedidos no sistema.");
        }
        return "redirect:/clientes";
    }
}
