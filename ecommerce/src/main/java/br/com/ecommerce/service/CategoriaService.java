package br.com.ecommerce.service;

import br.com.ecommerce.model.Categoria;
import br.com.ecommerce.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class CategoriaService {

    @Autowired
    private CategoriaRepository repository;

    public List<Categoria> listarTodos() {
        return repository.findAll();
    }

    public Optional<Categoria> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Categoria salvar(Categoria categoria) {
        return repository.save(categoria);
    }

    @Transactional
    public void excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Categoria não encontrada com o ID: " + id);
        }
        repository.deleteById(id);
    }
}
