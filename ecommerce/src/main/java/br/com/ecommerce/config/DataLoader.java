package br.com.ecommerce.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.com.ecommerce.model.Categoria;
import br.com.ecommerce.model.Cliente;
import br.com.ecommerce.model.Produto;
import br.com.ecommerce.model.Usuario;
import br.com.ecommerce.repository.CategoriaRepository;
import br.com.ecommerce.repository.ClienteRepository;
import br.com.ecommerce.repository.ProdutoRepository;
import br.com.ecommerce.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

<<<<<<< HEAD
    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== INICIANDO POPULAÇÃO IDEMPOTENTE DO BANCO DE DADOS ======");
=======
    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Criar usuários do sistema se não existirem
        if (usuarioRepository.count() == 0) {
            Usuario admin = new Usuario();
            admin.setEmail("admin@admin.com");
            admin.setSenha(passwordEncoder.encode("admin123"));
            admin.setRole("ROLE_ADMIN");
>>>>>>> 0134e271afe65384cd207ddd6f0cc728bf87e58c

        // 1. Criar usuários do sistema de forma segura
        criarUsuarioSeNaoExistir("admin@admin.com", "admin123", "ROLE_ADMIN");
        criarUsuarioSeNaoExistir("maria.silva@email.com", "cliente123", "ROLE_USER");
        criarUsuarioSeNaoExistir("joao.oliveira@email.com", "cliente123", "ROLE_USER");
        criarUsuarioSeNaoExistir("ana.souza@email.com", "cliente123", "ROLE_USER");

<<<<<<< HEAD
        // 2. Criar Categorias de forma segura
        Categoria eletronicos = criarCategoriaSeNaoExistir("Eletrônicos");
        Categoria roupas = criarCategoriaSeNaoExistir("Roupas");
        Categoria livros = criarCategoriaSeNaoExistir("Livros");
        Categoria games = criarCategoriaSeNaoExistir("Games");

        // 3. Criar Clientes de forma segura
        criarClienteSeNaoExistir("Maria Silva", "maria.silva@email.com", "123.456.789-00");
        criarClienteSeNaoExistir("João Oliveira", "joao.oliveira@email.com", "987.654.321-11");
        criarClienteSeNaoExistir("Ana Souza", "ana.souza@email.com", "456.789.123-22");

        // 4. Criar Produtos de forma segura
        criarProdutoSeNaoExistir("Smartphone Pro Max", "Tela de 6.7 polegadas, 256GB de armazenamento, câmera tripla de 48MP.", new BigDecimal("5999.00"), 10, eletronicos);
        criarProdutoSeNaoExistir("Notebook Ultra Slim", "Processador de última geração, 16GB RAM, SSD 512GB, tela de 14 polegadas.", new BigDecimal("4299.90"), 5, eletronicos);
        criarProdutoSeNaoExistir("Camiseta Algodão Egípcio", "Camiseta premium preta 100% algodão egípcio com toque super macio.", new BigDecimal("89.90"), 25, roupas);
        criarProdutoSeNaoExistir("Spring Boot da Prática ao Deploy", "Aprenda a construir APIs REST robustas, Spring MVC, Thymeleaf e banco de dados.", new BigDecimal("79.90"), 30, livros);
        criarProdutoSeNaoExistir("Console NextGen 8K", "Experimente o carregamento ultrarrápido com um SSD de velocidade incrível.", new BigDecimal("4499.00"), 3, games);

        System.out.println("====== BANCO DE DADOS ATUALIZADO COM SUCESSO ======");
    }

    private void criarUsuarioSeNaoExistir(String email, String senha, String role) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setSenha(passwordEncoder.encode(senha));
            usuario.setRole(role);
            usuarioRepository.save(usuario);
            System.out.println("Usuário criado: " + email);
=======
            usuarioRepository.saveAll(Arrays.asList(admin, cliente));
            System.out.println("====== USUÁRIOS DE EXEMPLO CRIADOS ======");
>>>>>>> 0134e271afe65384cd207ddd6f0cc728bf87e58c
        }
    }

    private Categoria criarCategoriaSeNaoExistir(String nome) {
        return categoriaRepository.findByNome(nome).orElseGet(() -> {
            Categoria categoria = new Categoria();
            categoria.setNome(nome);
            Categoria salva = categoriaRepository.save(categoria);
            System.out.println("Categoria criada: " + nome);
            return salva;
        });
    }

    private void criarClienteSeNaoExistir(String nome, String email, String cpf) {
        Optional<Cliente> clienteOpt = clienteRepository.findByEmail(email);
        if (clienteOpt.isEmpty()) {
            Cliente cliente = new Cliente();
            cliente.setNome(nome);
            cliente.setEmail(email);
            cliente.setCpf(cpf);
            clienteRepository.save(cliente);
            System.out.println("Cliente criado: " + nome + " (" + email + ")");
        }
    }

    private void criarProdutoSeNaoExistir(String nome, String descricao, BigDecimal preco, int estoque, Categoria categoria) {
        if (categoria == null) return;
        boolean existe = produtoRepository.findAll().stream()
                .anyMatch(p -> p.getNome().equalsIgnoreCase(nome));
        if (!existe) {
            Produto produto = new Produto();
            produto.setNome(nome);
            produto.setDescricao(descricao);
            produto.setPreco(preco);
            produto.setEstoque(estoque);
            produto.setCategoria(categoria);
            produtoRepository.save(produto);
            System.out.println("Produto criado: " + nome);
        }
    }
}
