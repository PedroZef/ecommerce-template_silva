package br.com.ecommerce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String index(Model model) {
        model.addAttribute("message", "Bem-vindo ao E-commerce!");
        return "dashboard";
    }
}
