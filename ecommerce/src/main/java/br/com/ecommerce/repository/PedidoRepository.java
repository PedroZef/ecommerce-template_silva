package br.com.ecommerce.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import br.com.ecommerce.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @Query("SELECT COALESCE(SUM(p.total), 0) FROM Pedido p")
    BigDecimal sumTotal();
}
