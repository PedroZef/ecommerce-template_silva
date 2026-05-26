package br.com.ecommerce.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Entity
@Table(name = "cliente")
@Data
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome do cliente é obrigatório.")
    @Column(nullable = false, length = 150)
    private String nome;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "Por favor, informe um e-mail válido.")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "O CPF é obrigatório.")
    @Size(min = 11, max = 14, message = "O CPF deve ter entre 11 e 14 caracteres.")
    @Column(nullable = false, unique = true, length = 14)
    private String cpf;
}
