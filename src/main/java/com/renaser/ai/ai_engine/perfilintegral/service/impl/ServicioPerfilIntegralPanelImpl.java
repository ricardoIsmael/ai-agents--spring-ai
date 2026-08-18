package com.renaser.ai.ai_engine.perfilintegral.service.impl;

import com.renaser.ai.ai_engine.ai.exception.ResourceNotFoundException;
import com.renaser.ai.ai_engine.ai.service.ColaCalificacionIa;
import com.renaser.ai.ai_engine.archivo.entity.Archivo;
import com.renaser.ai.ai_engine.archivo.service.AlmacenArchivos;
import com.renaser.ai.ai_engine.auditoria.service.ServicioAuditoria;
import com.renaser.ai.ai_engine.perfilintegral.dto.DtosPerfilIntegral.AlertaResponse;
import com.renaser.ai.ai_engine.perfilintegral.dto.DtosPerfilIntegral.CalificacionEncoladaResponse;
import com.renaser.ai.ai_engine.perfilintegral.dto.DtosPerfilIntegral.HallazgoResponse;
import com.renaser.ai.ai_engine.perfilintegral.dto.DtosPerfilIntegral.NotaCriterioResponse;
import com.renaser.ai.ai_engine.perfilintegral.dto.DtosPerfilIntegral.PerfilIntegralResponse;
import com.renaser.ai.ai_engine.perfilintegral.entity.Criterio;
import com.renaser.ai.ai_engine.perfilintegral.entity.NotaCriterio;
import com.renaser.ai.ai_engine.perfilintegral.entity.NotaEtapa;
import com.renaser.ai.ai_engine.perfilintegral.entity.PerfilTalento;
import com.renaser.ai.ai_engine.perfilintegral.repository.AlertaRepository;
import com.renaser.ai.ai_engine.perfilintegral.repository.CriterioRepository;
import com.renaser.ai.ai_engine.perfilintegral.repository.HallazgoPerfilRepository;
import com.renaser.ai.ai_engine.perfilintegral.repository.NotaCriterioRepository;
import com.renaser.ai.ai_engine.perfilintegral.repository.NotaEtapaRepository;
import com.renaser.ai.ai_engine.perfilintegral.repository.PerfilTalentoRepository;
import com.renaser.ai.ai_engine.perfilintegral.service.ServicioPerfilIntegralPanel;
import com.renaser.ai.ai_engine.postulacion.entity.Cv;
import com.renaser.ai.ai_engine.postulacion.entity.Postulacion;
import com.renaser.ai.ai_engine.postulacion.repository.CvRepository;
import com.renaser.ai.ai_engine.postulacion.repository.PostulacionRepository;
import com.renaser.ai.ai_engine.seguridad.dto.ContextoUsuario;
import com.renaser.ai.ai_engine.seguridad.dto.FiltroAlcance;
import com.renaser.ai.ai_engine.seguridad.service.Permisos;
import com.renaser.ai.ai_engine.vacante.repository.VacanteRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * El Perfil Integral para el panel: mirarlo y volver a pedirlo.
 *
 * <p>La etapa se cierra sola cuando el tercer agente termina, así que este servicio no
 * mueve a nadie de estado: solo lee lo que la IA dejó escrito y, si hace falta, vuelve a
 * poner el trabajo en la cola.
 */
@Service
@RequiredArgsConstructor
public class ServicioPerfilIntegralPanelImpl implements ServicioPerfilIntegralPanel {

    private static final String ETAPA = "PERFIL_INTEGRAL";

    private final PostulacionRepository postulaciones;
    private final VacanteRepository vacantes;
    private final PerfilTalentoRepository perfiles;
    private final HallazgoPerfilRepository hallazgos;
    private final NotaCriterioRepository notasCriterio;
    private final NotaEtapaRepository notasEtapa;
    private final AlertaRepository alertas;
    private final CriterioRepository criterios;
    private final CvRepository cvs;
    private final AlmacenArchivos almacen;
    private final ServicioAuditoria auditoria;
    private final ColaCalificacionIa cola;
    private final Permisos permisos;

    @Override
    public PerfilIntegralResponse ver(ContextoUsuario quien, Long postulacionId) {
        Postulacion postulacion = laVisible(quien, postulacionId, "ver_perfil_integral");
        PerfilTalento perfil = perfiles.findByPostulacionId(postulacionId).orElse(null);

        // Los ocho criterios del currículum mandan el orden y el máximo de cada uno. Se
        // parte de ellos y no de las notas: así un criterio sin calificar se ve como un
        // hueco, en vez de desaparecer de la lista como si no existiera.
        Map<Long, NotaCriterio> notaPorCriterio = notasCriterio.findByPostulacionId(postulacionId)
                .stream()
                .collect(Collectors.toMap(NotaCriterio::getCriterioId, Function.identity(),
                        (a, b) -> a));
        List<NotaCriterioResponse> notas = criterios
                .findByEtapaCodigoAndVersionPlantillaPruebaIdIsNullOrderByOrden(ETAPA).stream()
                .map(c -> pintarNota(c, notaPorCriterio.get(c.getId())))
                .toList();

        List<HallazgoResponse> lista = perfil == null ? List.of()
                : hallazgos.findByPerfilTalentoId(perfil.getId()).stream()
                        .map(h -> new HallazgoResponse(h.getTipo(), h.getDescripcion(),
                                h.getEvidencia(), h.isEsCanalizable(), h.getSugerencia()))
                        .toList();

        List<AlertaResponse> avisos = alertas.findByPostulacionId(postulacionId).stream()
                .map(a -> new AlertaResponse(a.getTipo(), a.getDescripcion(), a.getCreadoEn()))
                .toList();

        BigDecimal notaDeEtapa = notasEtapa.findByPostulacionIdAndEtapaCodigo(postulacionId, ETAPA)
                .map(NotaEtapa::getPuntaje).orElse(null);

        return new PerfilIntegralResponse(
                postulacionId,
                estadoDeLaCalificacion(postulacion, perfil),
                perfil == null ? null : perfil.getResumen(),
                perfil == null ? null : perfil.getAdecuacion(),
                perfil == null ? null : perfil.getPotencial(),
                perfil == null ? null : perfil.getAltoRendimiento(),
                perfil == null ? null : perfil.getConfianzaEvidencia(),
                notaDeEtapa,
                perfil == null ? null : perfil.getActualizadoEn(),
                lista, notas, avisos);
    }

    @Override
    @Transactional
    public CalificacionEncoladaResponse recalificar(ContextoUsuario quien, Long postulacionId) {
        Postulacion postulacion = laVisible(quien, postulacionId, "ajustar_nota");

        // Sin evaluación entregada no hay respuestas que calificar ni nota de lo cerrado
        // sobre la que apoyarse: encolar aquí solo produciría un trabajo condenado a fallar.
        if (postulacion.getEvaluacionId() == null) {
            throw new IllegalStateException(
                    "Esta postulación todavía no tiene evaluación: no hay nada que calificar");
        }

        cola.encolarPerfilIntegral(postulacionId);
        return new CalificacionEncoladaResponse("ENCOLADA",
                "La calificación quedó en cola. Tarda decenas de segundos: "
                        + "consulta el perfil para ver cuándo termina.");
    }

    @Override
    @Transactional
    public void reemplazarCv(ContextoUsuario quien, Long postulacionId, MultipartFile archivo) {
        laVisible(quien, postulacionId, "ajustar_nota");
        Cv curriculum = cvs.findByPostulacionId(postulacionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Currículum", "postulación", postulacionId));

        Archivo guardado = almacen.guardar(quien.organizacionId(), archivo);
        curriculum.setArchivoOriginalId(guardado.getId());
        // El texto viejo es de otro archivo: si se dejara, la IA calificaría el currículum
        // anterior creyendo que lee el nuevo. Se borra y se vuelve a extraer al calificar.
        curriculum.setTextoExtraido(null);
        curriculum.setTextoAnonimizado(null);
        curriculum.setAnonimizadoEn(null);
        cvs.save(curriculum);

        auditoria.registrar(quien.organizacionId(), quien, "reemplazar_cv",
                "cv", curriculum.getId(), null,
                Map.of("archivoOriginalId", String.valueOf(guardado.getId())), null);
    }

    // ============ Apoyo ============

    private NotaCriterioResponse pintarNota(Criterio criterio, NotaCriterio nota) {
        return new NotaCriterioResponse(
                criterio.getNombre(),
                nota == null ? null : nota.getPuntaje(),
                criterio.getPuntos(),
                nota == null ? null : nota.getExplicacion(),
                nota == null ? null : nota.getOrigen());
    }

    /**
     * En qué punto está la calificación, dicho para que una pantalla sepa qué pintar.
     *
     * <p><b>La señal son los trabajos de la cola, no el estado de la postulación.</b> Ese
     * estado solo pasa a {@code PERFIL_CALIFICANDO} cuando el candidato entrega su
     * evaluación, así que una calificación pedida desde el panel corre sin tocarlo: mirar la
     * postulación decía «no hay nada» con los tres agentes trabajando, y quien preguntaba
     * dejaba de esperar antes de tiempo.
     *
     * <p>Un trabajo vivo manda sobre el retrato ya guardado: si se volvió a calificar, el
     * perfil que hay es el viejo y darlo por bueno sería mentir.
     */
    private String estadoDeLaCalificacion(Postulacion postulacion, PerfilTalento perfil) {
        if (postulacion.getEvaluacionId() == null) {
            return "SIN_EVALUACION";
        }
        String segunLaCola = cola.comoVa(postulacion.getId());
        if ("EN_CURSO".equals(segunLaCola) || "FALLIDA".equals(segunLaCola)) {
            return segunLaCola;
        }
        // La cola dice que acabó: solo es TERMINADA si además dejó el retrato escrito.
        if ("TERMINADA".equals(segunLaCola)) {
            return perfil == null ? "FALLIDA" : "TERMINADA";
        }
        return perfil != null ? "TERMINADA" : "PENDIENTE";
    }

    /** La postulación, comprobando organización y alcance del permiso. */
    private Postulacion laVisible(ContextoUsuario quien, Long postulacionId, String permiso) {
        Postulacion p = postulaciones.findByIdAndOrganizacionId(postulacionId, quien.organizacionId())
                .orElseThrow(() -> new ResourceNotFoundException("Postulación", "id", postulacionId));
        FiltroAlcance alcance = permisos.alcanceDe(permiso);
        if (alcance.tipo() == FiltroAlcance.Tipo.SUS_VACANTES) {
            boolean esSuya = vacantes.findById(p.getVacanteId())
                    .map(v -> quien.usuarioId().equals(v.getResponsableUsuarioId()))
                    .orElse(false);
            if (!esSuya) {
                throw new ResourceNotFoundException("Postulación", "id", postulacionId);
            }
        }
        return p;
    }
}
