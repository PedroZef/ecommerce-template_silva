package br.com.ecommerce.repository;

import br.com.ecommerce.model.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    List<Despesa> findByCategoriaIgnoreCase(String categoria);

    @Query("SELECT SUM(d.valor) FROM Despesa d")
    BigDecimal calcularTotalDespesas();
}
