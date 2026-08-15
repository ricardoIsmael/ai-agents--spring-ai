package com.renaser.ai.ai_engine.reclutamiento.panel;

import com.renaser.ai.ai_engine.reclutamiento.panel.dto.DtosPanel.*;
import com.renaser.ai.ai_engine.reclutamiento.seguridad.ContextoUsuario;

import java.util.List;

public interface ServicioSolicitudes {

    // Talento prepara: la solicitud nace en BORRADOR
    Long crear(ContextoUsuario quien, CrearSolicitud datos);

    List<SolicitudResumen> listar(ContextoUsuario quien);

    SolicitudDetalle detalle(ContextoUsuario quien, Long id);

    // Dirección aprueba: BORRADOR -> ABIERTA. Solo una solicitud ABIERTA admite vacante.
    void aprobar(ContextoUsuario quien, Long id, String motivo);

    void rechazar(ContextoUsuario quien, Long id, String motivo);
}
