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

import br.com.ecommerce.model.Produto;
import br.com.ecommerce.service.CategoriaService;
import br.com.ecommerce.service.ProdutoService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;

    public ProdutoController(ProdutoService produtoService, CategoriaService categoriaService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public String listar(Model model,
                         @RequestParam(value = "success", required = false) String success,
                         @RequestParam(value = "error", required = false) String error) {
        model.addAttribute("produtos", produtoService.listarTodos());
        model.addAttribute("categorias", categoriaService.listarTodos());
        if (success != null) model.addAttribute("success", success);
        if (error != null) model.addAttribute("error", error);
        if (!model.containsAttribute("produto")) {
            model.addAttribute("produto", new Produto());
        }
        model.addAttribute("page", "produtos");
        return "produtos";
    }

    @PostMapping("/salvar")
    public String salvar(@Valid @ModelAttribute("produto") Produto produto,
            BindingResult result) {
        if (result.hasErrors()) {
            return "redirect:/produtos?error=Erro+ao+salvar+o+produto.+Verifique+se+os+dados+est%C3%A3o+preenchidos+corretamente.";
        }

        try {
            produtoService.salvar(produto);
            String nome = URLEncoder.encode(produto.getNome(), StandardCharsets.UTF_8);
            return "redirect:/produtos?success=Produto+" + nome + "+salvo+com+sucesso!";
        } catch (Exception e) {
            return "redirect:/produtos?error=Erro+ao+salvar+o+produto";
        }
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable("id") Long id, Model model) {
        try {
            Produto produto = produtoService.buscarPorId(id)
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado com o ID: " + id));
            model.addAttribute("produtos", produtoService.listarTodos());
            model.addAttribute("categorias", categoriaService.listarTodos());
            model.addAttribute("produto", produto);
            model.addAttribute("page", "produtos");
            return "produtos";
        } catch (Exception e) {
            return "redirect:/produtos?error=" + URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
        }
    }

    @GetMapping("/excluir/{id}")
    public String excluir(@PathVariable("id") Long id) {
        try {
            produtoService.excluir(id);
            return "redirect:/produtos?success=Produto+excluido+com+sucesso!";
        } catch (Exception e) {
            return "redirect:/produtos?error=Este+produto+n%C3%A3o+pode+ser+exclu%C3%ADdo+porque+est%C3%A1+associado+a+itens+de+pedidos+realizados.";
        }
    }
}
