package br.com.ecommerce.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
<<<<<<< HEAD
    public String index(HttpServletRequest request, Model model) {
        if (request.isUserInRole("ROLE_ADMIN")) {
            model.addAttribute("message", "Bem-vindo ao E-commerce!");
            return "dashboard";
        } else {
            return "redirect:/checkout";
        }
=======
    public String index() {
        return "redirect:/";
>>>>>>> 0134e271afe65384cd207ddd6f0cc728bf87e58c
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
