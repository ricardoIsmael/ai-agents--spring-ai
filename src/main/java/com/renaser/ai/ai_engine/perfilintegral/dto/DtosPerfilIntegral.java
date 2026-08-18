package com.renaser.ai.ai_engine.perfilintegral.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Lo que el panel enseña del Perfil Integral de un candidato.
 *
 * <p>Es de <b>solo lectura</b>: aquí no se ajusta ninguna nota. Cambiar una nota tiene su
 * propio camino, porque exige motivo escrito y deja rastro de quién la cambió.
 *
 * <p>Estos contratos son del panel del equipo, no del portal. El candidato nunca ve su
 * puntaje ni la explicación del modelo.
 */
public final class DtosPerfilIntegral {

    private DtosPerfilIntegral() {}

    /**
     * El retrato completo. Puede llegar a medias y eso es información, no un error: si la
     * IA todavía no ha corrido, {@code perfil} viene vacío y {@code estadoCalificacion}
     * dice por qué.
     */
    public record PerfilIntegralResponse(
            Long postulacionId,
            String estadoCalificacion,
            String resumen,
            BigDecimal adecuacion,
            BigDecimal potencial,
            BigDecimal altoRendimiento,
            BigDecimal confianzaEvidencia,
            BigDecimal notaEtapa,
            Instant actualizadoEn,
            List<HallazgoResponse> hallazgos,
            List<NotaCriterioResponse> notasCriterio,
            List<AlertaResponse> alertas) {}

    // El tipo no es decorativo: la Regla 1 del documento 03 prohíbe mezclarlos. Un riesgo
    // que se puede corregir y una falta de evidencia no son lo mismo, y el panel los pinta
    // distinto para que nadie los confunda al decidir.
    public record HallazgoResponse(String tipo, String descripcion, String evidencia,
                                   boolean esCanalizable, String sugerencia) {}

    // La explicación viaja siempre: una nota sin ella no se guarda, así que tampoco se
    // enseña sola. Quien revisa tiene que poder ver en qué se basó el modelo.
    public record NotaCriterioResponse(String criterio, BigDecimal puntaje,
                                       BigDecimal maximo, String explicacion, String origen) {}

    // Una alerta no descarta a nadie: es una pregunta para la conversación final.
    public record AlertaResponse(String tipo, String descripcion, Instant creadoEn) {}

    /** Lo que responde pedir que se vuelva a calificar. */
    public record CalificacionEncoladaResponse(String estado, String mensaje) {}
}
