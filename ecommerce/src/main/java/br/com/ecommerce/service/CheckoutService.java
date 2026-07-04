package br.com.ecommerce.service;

import br.com.ecommerce.exception.EstoqueInsuficienteException;
import br.com.ecommerce.model.*;
import br.com.ecommerce.repository.ClienteRepository;
import br.com.ecommerce.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final ProdutoRepository produtoRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoService pedidoService;

    public Cliente buscarCliente(Long clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado com o ID: " + clienteId));
    }

    @Transactional
    public Pedido processarCheckout(
            Long clienteId,
            List<Long> produtoIds,
            List<Integer> quantidades,
            MeioPagamento meioPagamento,
            Integer parcelas,
            String numCartao,
            String nomeCartao,
            String validadeCartao,
            String cvvCartao
    ) {
        if (produtoIds == null || quantidades == null || produtoIds.isEmpty()) {
            throw new IllegalArgumentException("Selecione pelo menos um produto.");
        }

        Cliente cliente = buscarCliente(clienteId);

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setMeioPagamento(meioPagamento);

        switch (meioPagamento) {
            case CARTAO_CREDITO -> {
                validarCartao(numCartao, nomeCartao, validadeCartao, cvvCartao);
                pedido.setParcelas(parcelas != null ? parcelas : 1);
                String cleanNum = numCartao != null ? numCartao.replaceAll("\\D", "") : "";
                String last4 = cleanNum.length() >= 4 ? cleanNum.substring(cleanNum.length() - 4) : "xxxx";
                pedido.setDetalhesPagamento(String.format("Crédito final %s - %dx", last4, pedido.getParcelas()));
            }
            case CARTAO_DEBITO -> {
                validarCartao(numCartao, nomeCartao, validadeCartao, cvvCartao);
                pedido.setParcelas(null);
                String cleanNum = numCartao != null ? numCartao.replaceAll("\\D", "") : "";
                String last4 = cleanNum.length() >= 4 ? cleanNum.substring(cleanNum.length() - 4) : "xxxx";
                pedido.setDetalhesPagamento(String.format("Débito final %s", last4));
            }
            case PIX -> {
                pedido.setParcelas(null);
                pedido.setDetalhesPagamento("Pix");
            }
            case BOLETO -> {
                pedido.setParcelas(null);
                pedido.setDetalhesPagamento("Aguardando pagamento em 3 dias úteis");
            }
        }

        for (int i = 0; i < produtoIds.size(); i++) {
            Long prodId = produtoIds.get(i);
            Integer qtd = quantidades.get(i);

            if (qtd == null || qtd <= 0) continue;

            Produto produto = produtoRepository.findById(prodId)
                    .orElseThrow(() -> new IllegalArgumentException("Produto não encontrado com o ID: " + prodId));

            ItemPedido item = new ItemPedido();
            item.setProduto(produto);
            item.setQuantidade(qtd);
            item.setPrecoUnitario(produto.getPreco());

            pedido.adicionarItem(item);
        }

        if (pedido.getItens().isEmpty()) {
            throw new IllegalArgumentException("Selecione pelo menos um produto com quantidade maior que zero.");
        }

        return pedidoService.criarPedido(pedido);
    }

    private void validarCartao(String numCartao, String nomeCartao, String validadeCartao, String cvvCartao) {
        if (nomeCartao == null || nomeCartao.isBlank()) {
            throw new IllegalArgumentException("O nome do titular do cartão é obrigatório.");
        }

        if (numCartao == null || numCartao.replaceAll("\\D", "").length() < 13) {
            throw new IllegalArgumentException("Número de cartão inválido. Deve conter ao menos 13 dígitos.");
        }

        if (cvvCartao == null || !cvvCartao.matches("\\d{3,4}")) {
            throw new IllegalArgumentException("CVV inválido. Deve conter 3 ou 4 dígitos.");
        }

        if (validadeCartao == null || !validadeCartao.matches("\\d{2}/\\d{2,4}")) {
            throw new IllegalArgumentException("Validade inválida. Use o formato MM/AA.");
        }

        String[] partes = validadeCartao.split("/");
        int mes = Integer.parseInt(partes[0]);
        int ano = Integer.parseInt(partes[1]);

        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("Mês da validade inválido: " + mes + ". Deve estar entre 01 e 12.");
        }

        // Normaliza ano para formato 20XX
        if (ano < 100) {
            ano += 2000;
        }

        YearMonth dataValidade = YearMonth.of(ano, mes);
        if (dataValidade.isBefore(YearMonth.now())) {
            throw new IllegalArgumentException("Cartão vencido. A validade informada (" + validadeCartao + ") já expirou.");
        }
    }
}