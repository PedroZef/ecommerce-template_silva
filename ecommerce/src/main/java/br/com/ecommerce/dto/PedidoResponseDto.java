package br.com.ecommerce.dto;

import br.com.ecommerce.model.Pedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PedidoResponseDto {
    private Long id;
    private String clienteNome;
    private String clienteEmail;
    private LocalDateTime dataPedido;
    private String status;
    private String meioPagamento;
    private Integer parcelas;
    private String detalhesPagamento;
    private BigDecimal total;
    private List<ItemPedidoResponseDto> itens;

    public PedidoResponseDto(Pedido pedido) {
        this.id = pedido.getId();
        this.clienteNome = pedido.getCliente().getNome();
        this.clienteEmail = pedido.getCliente().getEmail();
        this.dataPedido = pedido.getDataPedido();
        this.status = pedido.getStatus().name();
        this.meioPagamento = pedido.getMeioPagamento() != null ? pedido.getMeioPagamento().name() : null;
        this.parcelas = pedido.getParcelas();
        this.detalhesPagamento = pedido.getDetalhesPagamento();
        this.total = pedido.getTotal();
        this.itens = pedido.getItens().stream()
                .map(ItemPedidoResponseDto::new)
                .toList();
    }

    public Long getId() { return id; }
    public String getClienteNome() { return clienteNome; }
    public String getClienteEmail() { return clienteEmail; }
    public LocalDateTime getDataPedido() { return dataPedido; }
    public String getStatus() { return status; }
    public String getMeioPagamento() { return meioPagamento; }
    public Integer getParcelas() { return parcelas; }
    public String getDetalhesPagamento() { return detalhesPagamento; }
    public BigDecimal getTotal() { return total; }
    public List<ItemPedidoResponseDto> getItens() { return itens; }
}