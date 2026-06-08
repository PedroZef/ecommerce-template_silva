package br.com.ecommerce.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ThemeController {

    @PostMapping("/theme/toggle")
    public String toggleTheme(@RequestParam(value = "currentTheme", defaultValue = "dark") String currentTheme, 
                              HttpServletRequest request, 
                              HttpServletResponse response) {
                                  
        // Inverte o estado do tema
        String newTheme = "dark".equals(currentTheme) ? "light" : "dark";
        
        // Cria o cookie com o novo tema
        Cookie cookie = new Cookie("theme", newTheme);
        cookie.setPath("/"); // Funciona para o site inteiro (todas as telas)
        cookie.setMaxAge(30 * 24 * 60 * 60); // O navegador lembrará por 30 dias
        
        // Se usar HTTPS no futuro, é bom habilitar: cookie.setSecure(true);
        // cookie.setHttpOnly(true); // Opcional (impede leitura futura via JS, o que traz mais segurança)
        
        response.addCookie(cookie);

        // Retorna o usuário exatamente para a página de onde ele clicou
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/");
    }
}