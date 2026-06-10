package br.com.ecommerce.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class H2ConsoleForwardController {

    @GetMapping("/h2-console")
    public String forwardToH2Console() {
        // Redireciona a rota sem barra para a rota com barra,
        // garantindo que o Servlet do H2 seja ativado no Spring Boot 3+
        return "redirect:/h2-console/";
    }
}
