package br.com.ecommerce.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String index(HttpServletRequest request, Model model) {
        if (request.isUserInRole("ROLE_ADMIN")) {
            model.addAttribute("message", "Bem-vindo ao E-commerce!");
            return "dashboard";
        } else {
            return "redirect:/checkout";
        }
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
