package br.com.ecommerce.repository;

import br.com.ecommerce.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    @Query("SELECT COALESCE(SUM(p.total), 0) FROM Pedido p")
    BigDecimal sumTotal();
}
