package br.com.ecommerce.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.com.ecommerce.model.Cliente;
import br.com.ecommerce.service.ClienteService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model,
                         @RequestParam(value = "success", required = false) String success,
                         @RequestParam(value = "error", required = false) String error) {
        model.addAttribute("clientes", service.listarTodos());
        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);
        if (!model.containsAttribute("cliente")) {
            model.addAttribute("cliente", new Cliente());
        }
        model.addAttribute("page", "clientes");
        return "clientes";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("cliente") Cliente cliente,
            BindingResult result) {
        if (result.hasErrors()) {
            return "redirect:/clientes?error=Erro+ao+salvar+cliente.+Verifique+as+valida%C3%A7%C3%B5es+dos+campos.";
        }

        try {
            service.salvar(cliente);
            String nomeEncoded = URLEncoder.encode(cliente.getNome(), StandardCharsets.UTF_8);
            return "redirect:/clientes?success=Cliente+" + nomeEncoded + "+cadastrado+com+sucesso!";
        } catch (IllegalArgumentException e) {
            return "redirect:/clientes?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "redirect:/clientes?error=Erro+ao+salvar+cliente";
        }
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Long id, Model model) {
        try {
            Cliente cliente = service.buscarPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado com o ID: " + id));
            model.addAttribute("clientes", service.listarTodos());
            model.addAttribute("cliente", cliente);
            model.addAttribute("page", "clientes");
            return "clientes";
        } catch (Exception e) {
            return "redirect:/clientes?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Long id) {
        try {
            service.excluir(id);
            return "redirect:/clientes?success=Cliente+excluido+com+sucesso!";
        } catch (Exception e) {
            return "redirect:/clientes?error=Erro+ao+excluir+cliente";
        }
    }
}
