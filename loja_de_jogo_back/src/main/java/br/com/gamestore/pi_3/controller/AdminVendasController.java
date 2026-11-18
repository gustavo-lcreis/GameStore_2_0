package br.com.gamestore.pi_3.controller;

import br.com.gamestore.pi_3.dto.VendasResponse;
import br.com.gamestore.pi_3.service.VendaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
public class AdminVendasController {

    private final VendaService vendaService;

    public AdminVendasController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    /** GET /api/admin/vendas?inicio=2026-11-01&fim=2026-11-30&page=0&size=10 (todos os parâmetros são opcionais). */
    @GetMapping("/vendas")
    public VendasResponse listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return vendaService.listar(inicio, fim, page, size);
    }
}
