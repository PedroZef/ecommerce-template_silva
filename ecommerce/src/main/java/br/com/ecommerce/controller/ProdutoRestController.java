package br.com.ecommerce.controller;

import br.com.ecommerce.dto.CategoriaDTO;
import br.com.ecommerce.dto.ProdutoRequestDto;
import br.com.ecommerce.dto.ProdutoResponseDto;
import br.com.ecommerce.model.Categoria;
import br.com.ecommerce.model.Produto;
import br.com.ecommerce.service.CategoriaService;
import br.com.ecommerce.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

// Diferente de @Controller, o @RestController converte tudo para JSON automaticamente
// Prefixo /api para separar das rotas das páginas HTML
@RestController
@RequestMapping("/api/produtos")
public class ProdutoRestController {

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;

    public ProdutoRestController(ProdutoService produtoService, CategoriaService categoriaService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
    }

    // Retorna a lista de todos os produtos em JSON
    // Acessar: GET http://localhost:8080/api/produtos
    @GetMapping
    public List<ProdutoResponseDto> listarTodos() {
        return produtoService.listarTodos().stream()
                .map(this::convertToResponseDto)
                .toList();
    }

    // Retorna um único produto em JSON pelo ID
    // Acessar: GET http://localhost:8080/api/produtos/1
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDto> buscarPorId(@PathVariable("id") Long id) {
        return produtoService.buscarPorId(id)
                .map(produto -> ResponseEntity.ok(convertToResponseDto(produto)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Cria um novo produto a partir do JSON enviado no corpo da requisição
    // Acessar: POST http://localhost:8080/api/produtos
    @PostMapping
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProdutoResponseDto> criar(@Valid @RequestBody ProdutoRequestDto dto) {
        Categoria categoria = categoriaService.buscarPorId(dto.getCategoriaId())
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada com o ID: " + dto.getCategoriaId()));
        
        Produto produto = convertToEntity(dto);
        produto.setCategoria(categoria);
        
        Produto novoProduto = produtoService.salvar(produto);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToResponseDto(novoProduto));
    }

    // Atualiza um produto existente a partir do JSON e ID fornecidos
    // Acessar: PUT http://localhost:8080/api/produtos/1
    @PutMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProdutoResponseDto> atualizar(@PathVariable("id") Long id, @Valid @RequestBody ProdutoRequestDto dto) {
        try {
            Categoria categoria = categoriaService.buscarPorId(dto.getCategoriaId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada com o ID: " + dto.getCategoriaId()));
            
            Produto produtoAtualizado = convertToEntity(dto);
            produtoAtualizado.setCategoria(categoria);
            
            Produto produto = produtoService.atualizar(id, produtoAtualizado);
            return ResponseEntity.ok(convertToResponseDto(produto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Remove um produto pelo ID
    // Acessar: DELETE http://localhost:8080/api/produtos/1
    @DeleteMapping("/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> remover(@PathVariable("id") Long id) {
        try {
            produtoService.excluir(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private ProdutoResponseDto convertToResponseDto(Produto produto) {
        ProdutoResponseDto dto = new ProdutoResponseDto();
        dto.setId(produto.getId());
        dto.setNome(produto.getNome());
        dto.setDescricao(produto.getDescricao());
        dto.setPreco(produto.getPreco());
        dto.setEstoque(produto.getEstoque());
        if (produto.getCategoria() != null) {
            dto.setCategoria(new CategoriaDTO(produto.getCategoria().getId(), produto.getCategoria().getNome()));
        }
        return dto;
    }

    private Produto convertToEntity(ProdutoRequestDto dto) {
        Produto produto = new Produto();
        produto.setNome(dto.getNome());
        produto.setDescricao(dto.getDescricao());
        produto.setPreco(dto.getPreco());
        produto.setEstoque(dto.getEstoque());
        return produto;
    }
}