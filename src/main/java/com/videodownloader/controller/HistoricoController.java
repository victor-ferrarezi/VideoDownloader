package com.videodownloader.controller;

import com.videodownloader.dto.HistoricoResponse;
import com.videodownloader.service.HistoricoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/historico")
public class HistoricoController {

    private final HistoricoService historicoService;

    public HistoricoController(HistoricoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping
    public List<HistoricoResponse> listar() {
        return historicoService.listarRecentes().stream()
                .map(HistoricoResponse::from)
                .toList();
    }
}