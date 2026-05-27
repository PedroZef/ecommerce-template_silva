package br.com.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // Desabilita CSRF para permitir testes no Postman
                .authorizeHttpRequests((requests) -> requests
                        // 1. Arquivos estáticos liberados
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        // 2. Apenas a tela de login (e possivelmente produtos se quiser vitrine
                        // pública) são liberados
                        .requestMatchers("/login", "/produtos/", "/api/**").permitAll()
                        // 3. Páginas de compra (carrinho/checkout e pedidos) exigem usuário logado
                        .requestMatchers("/checkout/**", "/pedidos/**").authenticated()
                        // 4. (Opcional) Páginas de administração exigem papel de ADMIN
                        .requestMatchers("/categorias/**", "/clientes/**").hasRole("ADMIN")
                        // 5. O Restante (incluindo "/" e "/home") exige autenticação obrigatória
                        .anyRequest().authenticated())

                .formLogin((form) -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true)
                        .permitAll())
                .httpBasic(org.springframework.security.config.Customizer.withDefaults()) // Habilita Basic Auth para o
                                                                                          // Postman
                .logout((logout) -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
