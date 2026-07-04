package br.com.ecommerce.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.ecommerce.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @Query("SELECT COALESCE(SUM(p.total), 0) FROM Pedido p")
    BigDecimal sumTotal();

    @Query("SELECT COALESCE(SUM(p.total), 0) FROM Pedido p WHERE p.status = 'CONCLUIDO'")
    BigDecimal sumTotalConcluidos();

    List<Pedido> findByClienteId(Long clienteId);

    Page<Pedido> findAllByOrderByDataPedidoDesc(Pageable pageable);
}
