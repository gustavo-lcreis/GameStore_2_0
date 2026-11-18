package br.com.gamestore.pi_3.controller;

import br.com.gamestore.pi_3.dto.DashboardResponse;
import br.com.gamestore.pi_3.service.DashboardService;
import br.com.gamestore.pi_3.service.DashboardService.Agrupamento;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** /api/admin/** já exige ROLE_ADMIN no AuthInterceptor (ver WebConfig): nenhuma checagem extra aqui. */
@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    public AdminDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** GET /api/admin/dashboard?inicio=2026-11-01&fim=2026-11-30&agrupamento=DIA|MES&top=5 (tudo opcional). */
    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "DIA") Agrupamento agrupamento,
            @RequestParam(defaultValue = "5") int top) {
        return dashboardService.consolidar(inicio, fim, agrupamento, top);
    }
}
