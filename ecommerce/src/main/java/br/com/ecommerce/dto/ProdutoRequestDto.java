package br.com.ecommerce.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProdutoRequestDto {

    @NotBlank(message = "O nome do produto é obrigatório.")
    private String nome;

    private String descricao;

    @NotNull(message = "O preço é obrigatório.")
    @DecimalMin(value = "0.0", inclusive = true, message = "O preço do produto não pode ser negativo.")
    private BigDecimal preco;

    @NotNull(message = "A quantidade em estoque é obrigatória.")
    @Min(value = 0, message = "O estoque não pode ser negativo.")
    private Integer estoque;

    @NotNull(message = "O ID da categoria é obrigatório.")
    private Long categoriaId;
}
