package br.com.ecommerce.controller;

import br.com.ecommerce.model.Produto;
import br.com.ecommerce.service.ProdutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Diferente de @Controller, o @RestController converte tudo para JSON automaticamente
// Prefixo /api para separar das rotas das páginas HTML
@RestController
@RequestMapping("/api/produtos")
public class ProdutoRestController {

    @Autowired
    private ProdutoService produtoService;

    // Retorna a lista de todos os produtos em JSON
    // Acessar: GET http://localhost:8080/api/produtos
    @GetMapping
    public List<Produto> listarTodos() {
        return produtoService.listarTodos();
    }

    // Retorna um único produto em JSON pelo ID
    // Acessar: GET http://localhost:8080/api/produtos/1
    @GetMapping("/{id}")
    public ResponseEntity<Produto> buscarPorId(@PathVariable Long id) {
        return produtoService.buscarPorId(id)
                .map(produto -> ResponseEntity.ok().body(produto))
                .orElse(ResponseEntity.notFound().build()); // Retorna status 404 (Not Found) apropriado para API se não
                                                            // existir
    }
}