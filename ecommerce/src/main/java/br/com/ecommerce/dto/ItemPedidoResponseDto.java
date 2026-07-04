package br.com.ecommerce.dto;

import br.com.ecommerce.model.ItemPedido;
import java.math.BigDecimal;

public class ItemPedidoResponseDto {
    private Long id;
    private String produtoNome;
    private Integer quantidade;
    private BigDecimal precoUnitario;
    private BigDecimal subtotal;

    public ItemPedidoResponseDto(ItemPedido item) {
        this.id = item.getId();
        this.produtoNome = item.getProduto().getNome();
        this.quantidade = item.getQuantidade();
        this.precoUnitario = item.getPrecoUnitario();
        this.subtotal = item.getSubtotal();
    }

    public Long getId() { return id; }
    public String getProdutoNome() { return produtoNome; }
    public Integer getQuantidade() { return quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
}