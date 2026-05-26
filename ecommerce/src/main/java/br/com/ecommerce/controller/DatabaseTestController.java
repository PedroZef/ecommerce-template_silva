package br.com.ecommerce.controller;

import br.com.ecommerce.model.Categoria;
import br.com.ecommerce.service.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DatabaseTestController {

    @Autowired
    private CategoriaService service;

    @GetMapping("/test-db")
    public List<Categoria> testConnection() {
        return service.listarTodos();
    }
}

