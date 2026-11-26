package co.elchupacabras.tickets.pedidos.web;

import co.elchupacabras.tickets.pedidos.application.ValidadorDisponibilidadService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/solicitudes")
public class SolicitudController {

    private final ValidadorDisponibilidadService validadorDisponibilidadService;

    public SolicitudController(ValidadorDisponibilidadService validadorDisponibilidadService) {
        this.validadorDisponibilidadService = validadorDisponibilidadService;
    }

    @PostMapping("/validar")
    public ResponseEntity<SolicitudValidarResponse> validarSolicitud(
            @Valid @RequestBody SolicitudValidarRequest request
    ) {
        return ResponseEntity.ok(validadorDisponibilidadService.validarSolicitud(request));
    }
}
