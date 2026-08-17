package com.renaser.ai.ai_engine.postulacion.controller;

import com.renaser.ai.ai_engine.postulacion.service.ServicioPostulacionesPanel;

import com.renaser.ai.ai_engine.postulacion.dto.DtosPostulacion.*;
import com.renaser.ai.ai_engine.seguridad.service.Permisos;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/panel")
@RequiredArgsConstructor
@Tag(name = "Panel · Postulaciones", description = "La bandeja, la ficha y los movimientos")
public class PostulacionesPanelController {

    private final ServicioPostulacionesPanel servicio;
    private final Permisos permisos;

    @GetMapping("/bandeja")
    @PreAuthorize("@permisos.tiene('ver_candidatos')")
    @Operation(summary = "La bandeja de trabajo: todo lo que espera a alguien "
            + "(espera_a = CANDIDATO, SISTEMA, TALENTO o AREA)")
    public List<FilaBandeja> bandeja(@RequestParam("espera_a") String esperaA) {
        return servicio.bandeja(permisos.actual(), esperaA);
    }

    @GetMapping("/vacantes/{id}/embudo")
    @PreAuthorize("@permisos.tiene('ver_embudo')")
    @Operation(summary = "Cuántas postulaciones hay en cada estado de una vacante")
    public ConteoEmbudo embudo(@PathVariable Long id) {
        return servicio.embudo(permisos.actual(), id);
    }

    @GetMapping("/postulaciones/{id}")
    @PreAuthorize("@permisos.tiene('abrir_ficha_candidato')")
    @Operation(summary = "La ficha completa de una postulación")
    public FichaPostulacion ficha(@PathVariable Long id) {
        return servicio.ficha(permisos.actual(), id);
    }

    @GetMapping("/postulaciones/{id}/historial")
    @PreAuthorize("@permisos.tiene('abrir_ficha_candidato')")
    @Operation(summary = "El recorrido completo: cada transición, quién y por qué")
    public List<PasoHistorial> historial(@PathVariable Long id) {
        return servicio.historial(permisos.actual(), id);
    }

    @PostMapping("/postulaciones/{id}/transiciones")
    @PreAuthorize("@permisos.tiene('mover_postulacion')")
    @Operation(summary = "Mover a cualquier estado, con motivo OBLIGATORIO. "
            + "Si el destino es un cierre, motivoCierre dice de qué clase")
    public void transicionar(@PathVariable Long id, @Valid @RequestBody Transicionar datos) {
        servicio.transicionar(permisos.actual(), id, datos);
    }

    @PostMapping("/postulaciones/{id}/confirmacion-avance")
    @PreAuthorize("@permisos.tiene('confirmar_avance')")
    @Operation(summary = "Confirmar que avanza: aplica el estado siguiente que calcula la máquina")
    public void confirmarAvance(@PathVariable Long id, @Valid @RequestBody ConfirmarAvance datos) {
        servicio.confirmarAvance(permisos.actual(), id, datos.motivo());
    }

    @GetMapping("/archivos/{id}/descarga")
    @PreAuthorize("@permisos.tiene('descargar_entregables')")
    @Operation(summary = "Descargar un archivo (el CV) por su id")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id) {
        StringBuilder nombre = new StringBuilder();
        byte[] contenido = servicio.descargarArchivo(permisos.actual(), id, nombre);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(contenido);
    }
}
