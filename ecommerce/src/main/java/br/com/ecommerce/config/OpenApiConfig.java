package br.com.ecommerce.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "E-Commerce API Premium",
        version = "v1",
        description = "API para gestão de catálogo de produtos, faturamento e assistente de inteligência artificial."
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "Autenticação baseada em token JWT. Insira o token gerado para autorizar as chamadas protegidas."
)
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("1 - Geral (Público)")
                .pathsToMatch("/api/auth/**", "/api/produtos", "/api/produtos/**", "/api/pedidos", "/api/pedidos/**")
                .build();
    }

    @Bean
    public GroupedOpenApi privateApi() {
        return GroupedOpenApi.builder()
                .group("2 - Assistente IA (Protegido - Token)")
                .pathsToMatch("/api/assistant", "/api/assistant/**")
                .build();
    }
}
