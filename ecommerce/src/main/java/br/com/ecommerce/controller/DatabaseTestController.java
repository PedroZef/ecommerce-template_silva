package br.com.ecommerce.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.ecommerce.model.Categoria;
import br.com.ecommerce.service.CategoriaService;

@RestController
public class DatabaseTestController {

    private final CategoriaService service;

    public DatabaseTestController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping("/test-db")
    public List<Categoria> testConnection() {
        return service.listarTodos();
    }
}
