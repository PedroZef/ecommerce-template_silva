package br.com.ecommerce.controller;

import br.com.ecommerce.model.Produto;
import br.com.ecommerce.service.CategoriaService;
import br.com.ecommerce.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/produtos")
public class ProdutoController {

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private CategoriaService categoriaService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("produtos", produtoService.listarTodos());
        model.addAttribute("categorias", categoriaService.listarTodos());
        if (!model.containsAttribute("produto")) {
            model.addAttribute("produto", new Produto());
        }
        model.addAttribute("page", "produtos");
        return "produtos";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("produto") Produto produto,
                         BindingResult result,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.produto", result);
            redirectAttributes.addFlashAttribute("produto", produto);
            redirectAttributes.addFlashAttribute("error", "Erro ao salvar o produto. Verifique se os dados estão preenchidos corretamente.");
            return "redirect:/produtos";
        }

        try {
            produtoService.salvar(produto);
            redirectAttributes.addFlashAttribute("success", "Produto '" + produto.getNome() + "' salvo com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao salvar o produto: " + e.getMessage());
        }
        return "redirect:/produtos";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Produto produto = produtoService.buscarPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado com o ID: " + id));
            model.addAttribute("produtos", produtoService.listarTodos());
            model.addAttribute("categorias", categoriaService.listarTodos());
            model.addAttribute("produto", produto);
            model.addAttribute("page", "produtos");
            return "produtos";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/produtos";
        }
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            produtoService.excluir(id);
            redirectAttributes.addFlashAttribute("success", "Produto excluído com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Este produto não pode ser excluído porque está associado a itens de pedidos realizados.");
        }
        return "redirect:/produtos";
    }
}
