package br.com.ecommerce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    public String listar(Model model) {
        model.addAttribute("categorias", service.listarTodos());
        if (!model.containsAttribute("categoria")) {
            model.addAttribute("categoria", new Categoria());
        }
        model.addAttribute("page", "categorias");
        return "categorias";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("categoria") Categoria categoria,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.categoria", result);
            redirectAttributes.addFlashAttribute("categoria", categoria);
            redirectAttributes.addFlashAttribute("error", "Erro ao salvar a categoria. Verifique o formulário.");
            return "redirect:/categorias";
        }

        try {
            service.salvar(categoria);
            redirectAttributes.addFlashAttribute("success",
                    "Categoria '" + categoria.getNome() + "' salva com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao salvar categoria: " + e.getMessage());
        }
        return "redirect:/categorias";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Categoria categoria = service.buscarPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada com o ID: " + id));
            model.addAttribute("categorias", service.listarTodos());
            model.addAttribute("categoria", categoria);
            model.addAttribute("page", "categorias");
            return "categorias";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/categorias";
        }
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            service.excluir(id);
            redirectAttributes.addFlashAttribute("success", "Categoria excluída com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Esta categoria não pode ser excluída porque possui produtos associados a ela.");
        }
        return "redirect:/categorias";
    }
}
