package br.com.ecommerce.service;

import br.com.ecommerce.model.Cliente;
import br.com.ecommerce.repository.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    @Autowired
    private ClienteRepository repository;

    public List<Cliente> listarTodos() {
        return repository.findAll();
    }

    public Optional<Cliente> buscarPorId(Long id) {
        return repository.findById(id);
    }

    @Transactional
    public Cliente salvar(Cliente cliente) {
        // Validação de e-mail único
        Optional<Cliente> clienteEmail = repository.findByEmail(cliente.getEmail());
        if (clienteEmail.isPresent() && !clienteEmail.get().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("O e-mail informado já está cadastrado para outro cliente.");
        }

        // Validação de CPF único
        Optional<Cliente> clienteCpf = repository.findByCpf(cliente.getCpf());
        if (clienteCpf.isPresent() && !clienteCpf.get().getId().equals(cliente.getId())) {
            throw new IllegalArgumentException("O CPF informado já está cadastrado para outro cliente.");
        }

        return repository.save(cliente);
    }

    @Transactional
    public void excluir(Long id) {
        if (!repository.existsById(id)) {
            throw new IllegalArgumentException("Cliente não encontrado com o ID: " + id);
        }
        repository.deleteById(id);
    }
}
