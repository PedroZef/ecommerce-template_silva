package br.com.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "produto")
@Data
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do produto é obrigatório.")
    @Column(nullable = false, length = 150)
    private String nome;

    @Column(length = 500)
    private String descricao;

    @NotNull(message = "O preço é obrigatório.")
    @DecimalMin(value = "0.0", inclusive = true, message = "O preço do produto não pode ser negativo.")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @NotNull(message = "A quantidade em estoque é obrigatória.")
    @Min(value = 0, message = "O estoque não pode ser negativo.")
    @Column(nullable = false)
    private Integer estoque;

    @NotNull(message = "A categoria é obrigatória.")
    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;
}
