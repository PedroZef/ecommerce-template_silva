package br.com.ecommerce.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import br.com.ecommerce.model.Categoria;
import br.com.ecommerce.model.Cliente;
import br.com.ecommerce.model.Produto;
import br.com.ecommerce.repository.CategoriaRepository;
import br.com.ecommerce.repository.ClienteRepository;
import br.com.ecommerce.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Override
    public void run(String... args) throws Exception {
        if (categoriaRepository.count() == 0) {
            System.out.println("====== POPULANDO BANCO DE DADOS COM DADOS DE EXEMPLO ======");

            // 1. Criar Categorias
            Categoria eletronicos = new Categoria();
            eletronicos.setNome("Eletrônicos");

            Categoria roupas = new Categoria();
            roupas.setNome("Roupas");

            Categoria livros = new Categoria();
            livros.setNome("Livros");

            Categoria games = new Categoria();
            games.setNome("Games");

            categoriaRepository.saveAll(Arrays.asList(eletronicos, roupas, livros, games));

            // 2. Criar Produtos
            Produto smartphone = new Produto();
            smartphone.setNome("Smartphone Pro Max");
            smartphone.setDescricao("Tela de 6.7 polegadas, 256GB de armazenamento, câmera tripla de 48MP.");
            smartphone.setPreco(new BigDecimal("5999.00"));
            smartphone.setEstoque(10);
            smartphone.setCategoria(eletronicos);

            Produto notebook = new Produto();
            notebook.setNome("Notebook Ultra Slim");
            notebook.setDescricao("Processador de última geração, 16GB RAM, SSD 512GB, tela de 14 polegadas.");
            notebook.setPreco(new BigDecimal("4299.90"));
            notebook.setEstoque(5);
            notebook.setCategoria(eletronicos);

            Produto camiseta = new Produto();
            camiseta.setNome("Camiseta Algodão Egípcio");
            camiseta.setDescricao("Camiseta premium preta 100% algodão egípcio com toque super macio.");
            camiseta.setPreco(new BigDecimal("89.90"));
            camiseta.setEstoque(25);
            camiseta.setCategoria(roupas);

            Produto livroSpring = new Produto();
            livroSpring.setNome("Spring Boot da Prática ao Deploy");
            livroSpring.setDescricao("Aprenda a construir APIs REST robustas, Spring MVC, Thymeleaf e banco de dados.");
            livroSpring.setPreco(new BigDecimal("79.90"));
            livroSpring.setEstoque(30);
            livroSpring.setCategoria(livros);

            Produto consoleGame = new Produto();
            consoleGame.setNome("Console NextGen 8K");
            consoleGame.setDescricao("Experimente o carregamento ultrarrápido com um SSD de velocidade incrível.");
            consoleGame.setPreco(new BigDecimal("4499.00"));
            consoleGame.setEstoque(3);
            consoleGame.setCategoria(games);

            produtoRepository.saveAll(Arrays.asList(smartphone, notebook, camiseta, livroSpring, consoleGame));

            // 3. Criar Clientes
            Cliente c1 = new Cliente();
            c1.setNome("Maria Silva");
            c1.setEmail("maria.silva@email.com");
            c1.setCpf("123.456.789-00");

            Cliente c2 = new Cliente();
            c2.setNome("João Oliveira");
            c2.setEmail("joao.oliveira@email.com");
            c2.setCpf("987.654.321-11");

            Cliente c3 = new Cliente();
            c3.setNome("Ana Souza");
            c3.setEmail("ana.souza@email.com");
            c3.setCpf("456.789.123-22");

            clienteRepository.saveAll(Arrays.asList(c1, c2, c3));

            System.out.println("====== DADOS DE EXEMPLO POPULADOS COM SUCESSO ======");
        }
    }
}
