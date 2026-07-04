package br.com.ecommerce.controller;

import br.com.ecommerce.dto.PedidoResponseDto;
import br.com.ecommerce.model.Pedido;
import br.com.ecommerce.service.PedidoService;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
@SecurityRequirement(name = "bearerAuth")
public class PedidoRestController {

    private final PedidoService pedidoService;

    public PedidoRestController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping
    @Secured("ROLE_ADMIN")
    public List<PedidoResponseDto> listarTodos() {
        return pedidoService.listarTodos().stream()
                .map(PedidoResponseDto::new)
                .toList();
    }
}
