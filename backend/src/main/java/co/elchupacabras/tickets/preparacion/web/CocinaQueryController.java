package co.elchupacabras.tickets.preparacion.web;

import co.elchupacabras.tickets.preparacion.application.ConsultaComandasCocinaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cocina")
public class CocinaQueryController {

    private final ConsultaComandasCocinaService consultaComandasCocinaService;

    public CocinaQueryController(ConsultaComandasCocinaService consultaComandasCocinaService) {
        this.consultaComandasCocinaService = consultaComandasCocinaService;
    }

    @GetMapping("/comandas")
    public ResponseEntity<List<ComandaCocinaResponse>> obtenerComandasEnPreparacion(
            @RequestParam("jornadaId") Long jornadaId
    ) {
        return ResponseEntity.ok(consultaComandasCocinaService.consultarComandasEnPreparacion(jornadaId));
    }
}
