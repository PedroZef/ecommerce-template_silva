package br.com.ecommerce.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descricao;

    private BigDecimal valor;

    private String categoria; // Ex: "Supermercado", "Farmácia", "Outros"

    private LocalDateTime dataDespesa;

    @PrePersist
    protected void onCreate() {
        this.dataDespesa = LocalDateTime.now();
    }

    public Despesa(String descricao, BigDecimal valor, String categoria) {
        this.descricao = descricao;
        this.valor = valor;
        this.categoria = categoria;
    }
}
