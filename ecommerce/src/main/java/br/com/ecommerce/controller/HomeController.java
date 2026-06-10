package br.com.ecommerce.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/home")
    public String index() {
        return "redirect:/";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
