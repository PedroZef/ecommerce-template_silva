package br.com.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.web.servlet.ServletRegistrationBean;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Proteção CSRF ignorando endpoints de API e H2 Console
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**", "/h2-console", "/h2-console/**")
                )
                // Permite o uso de iframes para o console do banco H2
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .authorizeHttpRequests((requests) -> requests
                        // 1. Arquivos estáticos liberados
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        // 2. Apenas a tela de login, API e console H2 são liberados publicamente
                        .requestMatchers("/login", "/api/**", "/h2-console", "/h2-console/**").permitAll()
                        // 3. Páginas de compra (carrinho/checkout e pedidos) exigem usuário logado
                        .requestMatchers("/checkout", "/checkout/**", "/pedidos", "/pedidos/**").authenticated()
                        // 4. Páginas de administração exigem papel de ADMIN
                        .requestMatchers("/categorias/**", "/clientes/**", "/produtos/**", "/produtos").hasRole("ADMIN")
                        // 5. O Restante (incluindo "/" e "/home") exige autenticação obrigatória
                        .anyRequest().authenticated())

                .formLogin((form) -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/home", true)
                        .permitAll())
                .httpBasic(org.springframework.security.config.Customizer.withDefaults()) // Habilita Basic Auth para o Postman
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

    @Bean
    public ServletRegistrationBean<?> h2ConsoleServletRegistration() {
        try {
            Class<?> webServletClass = Class.forName("org.h2.server.web.JakartaWebServlet");
            jakarta.servlet.Servlet servlet = (jakarta.servlet.Servlet) webServletClass.getDeclaredConstructor().newInstance();
            ServletRegistrationBean<?> registration = new ServletRegistrationBean<>(servlet);
            registration.addUrlMappings("/h2-console/*");
            return registration;
        } catch (Exception e) {
            // Se o H2 não estiver no classpath (ex: em prod), não faz nada
            return null;
        }
    }
}
