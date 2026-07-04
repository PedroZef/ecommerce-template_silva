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

import br.com.ecommerce.model.Categoria;
import br.com.ecommerce.service.CategoriaService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping
    public String listar(Model model,
                         @RequestParam(value = "success", required = false) String success,
                         @RequestParam(value = "error", required = false) String error) {
        model.addAttribute("categorias", service.listarTodos());
        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);
        if (!model.containsAttribute("categoria")) {
            model.addAttribute("categoria", new Categoria());
        }
        model.addAttribute("page", "categorias");
        return "categorias";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("categoria") Categoria categoria,
            BindingResult result) {
        if (result.hasErrors()) {
            return "redirect:/categorias?error=Erro+ao+salvar+a+categoria.+Verifique+o+formul%C3%A1rio.";
        }

        try {
            service.salvar(categoria);
            String nome = URLEncoder.encode(categoria.getNome(), StandardCharsets.UTF_8);
            return "redirect:/categorias?success=Categoria+" + nome + "+salva+com+sucesso!";
        } catch (Exception e) {
            return "redirect:/categorias?error=Erro+ao+salvar+categoria";
        }
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Long id, Model model) {
        try {
            Categoria categoria = service.buscarPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada com o ID: " + id));
            model.addAttribute("categorias", service.listarTodos());
            model.addAttribute("categoria", categoria);
            model.addAttribute("page", "categorias");
            return "categorias";
        } catch (Exception e) {
            return "redirect:/categorias?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Long id) {
        try {
            service.excluir(id);
            return "redirect:/categorias?success=Categoria+excluida+com+sucesso!";
        } catch (Exception e) {
            return "redirect:/categorias?error=Esta+categoria+n%C3%A3o+pode+ser+exclu%C3%ADda+porque+possui+produtos+associados+a+ela.";
        }
    }
}
