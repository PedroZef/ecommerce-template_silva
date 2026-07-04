package br.com.ecommerce.service;

import br.com.ecommerce.exception.EstoqueInsuficienteException;
import br.com.ecommerce.model.ItemPedido;
import br.com.ecommerce.model.MeioPagamento;
import br.com.ecommerce.model.OrderStatus;
import br.com.ecommerce.model.Pedido;
import br.com.ecommerce.model.Produto;
import br.com.ecommerce.repository.PedidoRepository;
import br.com.ecommerce.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProdutoRepository produtoRepository;

    public long contarTodos() {
        return pedidoRepository.count();
    }

    public BigDecimal calcularFaturamentoTotal() {
        return pedidoRepository.sumTotal();
    }

    public BigDecimal calcularFaturamentoTotalConcluidos() {
        return pedidoRepository.sumTotalConcluidos();
    }

    public List<Pedido> listarTodos() {
        return pedidoRepository.findAll();
    }

    public List<Pedido> listarPorClienteId(Long clienteId) {
        return pedidoRepository.findByClienteId(clienteId);
    }

    public List<Pedido> listarRecentes(int limite) {
        return pedidoRepository.findAllByOrderByDataPedidoDesc(PageRequest.of(0, limite)).getContent();
    }

    public Optional<Pedido> buscarPorId(Long id) {
        return pedidoRepository.findById(id);
    }

    /**
     * Cria um pedido de forma atômica e consistente.
     * Caso o estoque de QUALQUER produto do carrinho seja insuficiente,
     * lança uma EstoqueInsuficienteException que dispara o ROLLBACK total automático
     * da transação, mantendo os estoques originais intocados no banco de dados.
     */
    @Transactional
    public Pedido criarPedido(Pedido pedido) {
        if (pedido.getItens() == null || pedido.getItens().isEmpty()) {
            throw new IllegalArgumentException("O pedido não pode ser finalizado sem itens na cesta.");
        }

        // Define o status conforme a forma de pagamento
        if (pedido.getMeioPagamento() == null) {
            pedido.setStatus(OrderStatus.CONCLUIDO);
        } else {
            pedido.setStatus(switch (pedido.getMeioPagamento()) {
                case PIX, CARTAO_DEBITO -> OrderStatus.CONCLUIDO;
                case CARTAO_CREDITO -> (pedido.getParcelas() != null && pedido.getParcelas() > 1)
                        ? OrderStatus.PENDENTE
                        : OrderStatus.CONCLUIDO;
                case BOLETO -> OrderStatus.PENDENTE;
            });
        }

        // Processa cada item do pedido
        for (ItemPedido item : pedido.getItens()) {
            // 1. Busca o produto
            Produto produto = produtoRepository.findById(item.getProduto().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Produto de ID " + item.getProduto().getId() + " não foi encontrado."));

            // 2. Registra o preço de compra histórico
            item.setPrecoUnitario(produto.getPreco());
            item.setPedido(pedido);

            // 3. Valida a disponibilidade em estoque
            if (produto.getEstoque() < item.getQuantidade()) {
                throw new EstoqueInsuficienteException(
                        String.format("Transação Cancelada (Rollback)! Estoque insuficiente para o produto '%s'. Disponível: %d, Solicitado: %d.",
                                produto.getNome(), produto.getEstoque(), item.getQuantidade())
                );
            }

            // 4. Deduz o estoque temporariamente
            produto.setEstoque(produto.getEstoque() - item.getQuantidade());
            produtoRepository.save(produto);
        }

        // Recalcula o total final com base nos preços históricos coletados
        pedido.recalcularTotal();

        // 5. Salva o pedido e os itens em cascata
        return pedidoRepository.save(pedido);
    }

    @Transactional
    public Pedido atualizarStatus(Long id, OrderStatus novoStatus) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Pedido não encontrado com o ID: " + id));
        pedido.setStatus(novoStatus);
        return pedidoRepository.save(pedido);
    }
}
