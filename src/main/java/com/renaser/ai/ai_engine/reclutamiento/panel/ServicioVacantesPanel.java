package com.renaser.ai.ai_engine.reclutamiento.panel;

import com.renaser.ai.ai_engine.reclutamiento.panel.dto.DtosPanel.*;
import com.renaser.ai.ai_engine.reclutamiento.seguridad.ContextoUsuario;

import java.util.List;

public interface ServicioVacantesPanel {

    Long crearPuesto(ContextoUsuario quien, GuardarPuesto datos);

    List<GuardarPuesto> listarPuestos(ContextoUsuario quien);

    // Crear exige una solicitud ABIERTA (aprobada por Dirección). Nace en BORRADOR.
    Long crear(ContextoUsuario quien, GuardarVacante datos);

    void editar(ContextoUsuario quien, Long id, GuardarVacante datos);

    List<VacantePanel> listar(ContextoUsuario quien);

    VacantePanel detalle(ContextoUsuario quien, Long id);

    List<RequisitoPanel> requisitos(ContextoUsuario quien, Long vacanteId);

    Long agregarRequisito(ContextoUsuario quien, Long vacanteId, GuardarRequisito datos);

    void desactivarRequisito(ContextoUsuario quien, Long vacanteId, Long requisitoId);

    void publicar(ContextoUsuario quien, Long id);

    // Cerrar detiene postulaciones nuevas; las que están en marcha se deciden una a una
    void cerrar(ContextoUsuario quien, Long id, String motivo);
}
