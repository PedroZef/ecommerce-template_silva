package br.com.ecommerce.service;

import br.com.ecommerce.model.Produto;
import br.com.ecommerce.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ProdutoService {

    @Autowired
    private ProdutoRepository repository;

    public List<Produto> listarTodos() {
        return repository.findAll();
    }

    public Optional<Produto> buscarPorId(Long id) {
        return repository.findById(id);
    }

    public List<Produto> listarPorCategoria(Long categoriaId) {
        return repository.findByCategoriaId(categoriaId);
    }

    @Transactional
    public Produto salvar(Produto produto) {
        return repository.save(produto);
    }

    @Transactional
    public void excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Produto não encontrado com o ID: " + id);
        }
        repository.deleteById(id);
    }
}
