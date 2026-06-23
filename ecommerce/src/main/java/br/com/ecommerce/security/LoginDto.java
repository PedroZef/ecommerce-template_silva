package br.com.ecommerce.security;

import lombok.Data;

@Data
public class LoginDto {
    private String username; // Email do usuário
    private String password;
}
