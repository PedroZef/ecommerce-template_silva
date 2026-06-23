package br.com.ecommerce.security;

import lombok.Data;

@Data
public class RegisterDto {
    private String email;
    private String password;
    private String role; // Ex: ROLE_USER ou ROLE_ADMIN
}
