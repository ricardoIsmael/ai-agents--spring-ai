package com.renaser.ai.ai_engine.ai.service.impl;

import com.renaser.ai.ai_engine.ai.messaging.TrabajoIaPublisher;
import com.renaser.ai.ai_engine.ai.model.TrabajoIa;
import com.renaser.ai.ai_engine.ai.repository.TrabajoIaRepository;
import com.renaser.ai.ai_engine.ai.service.AgenteSeleccion;
import com.renaser.ai.ai_engine.ai.service.ColaCalificacionIa;
import com.renaser.ai.ai_engine.perfilintegral.service.PuenteCalificacionIa;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Ver {@link ColaCalificacionIa}. */
@Service
@Slf4j
public class ColaCalificacionIaImpl implements ColaCalificacionIa {

    /**
     * El orden de la fila. Es una lista y no un campo en la base porque es una decisión de
     * diseño, no de configuración: el Perfil de Talento no puede armarse antes de que estén
     * las notas que resume, y eso no lo cambia nadie desde un panel.
     */
    private static final List<String> ORDEN = List.of(
            AgenteEvidenciaCv.CODIGO, AgenteEvaluador.CODIGO, AgentePotencialRiesgo.CODIGO);

    private final TrabajoIaRepository trabajos;
    private final RegistroTrabajosIa registro;
    private final TrabajoIaPublisher publicador;
    private final PuenteCalificacionIa puente;
    private final Map<String, AgenteSeleccion> agentes;
    private final boolean habilitada;
    private final int maxIntentos;
    private final Duration limiteColgado;

    public ColaCalificacionIaImpl(TrabajoIaRepository trabajos,
                                  RegistroTrabajosIa registro,
                                  TrabajoIaPublisher publicador,
                                  PuenteCalificacionIa puente,
                                  List<AgenteSeleccion> agentes,
                                  @Value("${renaser.ai.calificacion.habilitada:true}") boolean habilitada,
                                  @Value("${renaser.ai.calificacion.max-intentos:3}") int maxIntentos,
                                  @Value("${renaser.ai.calificacion.minutos-colgado:15}") int minutosColgado) {
        this.trabajos = trabajos;
        this.registro = registro;
        this.publicador = publicador;
        this.puente = puente;
        this.agentes = agentes.stream()
                .collect(Collectors.toMap(AgenteSeleccion::codigo, Function.identity()));
        this.habilitada = habilitada;
        this.maxIntentos = maxIntentos;
        this.limiteColgado = Duration.ofMinutes(minutosColgado);
    }

    @Override
    public void encolarPerfilIntegral(Long postulacionId) {
        if (!habilitada) {
            log.warn("La calificación con IA está apagada por configuración: la postulación {} "
                    + "se queda en PERFIL_CALIFICANDO", postulacionId);
            return;
        }
        encolar(postulacionId, ORDEN.get(0));
    }

    @Override
    public void ejecutar(Long trabajoIaId) {
        Optional<TrabajoIa> tomado = registro.tomar(trabajoIaId);
        if (tomado.isEmpty()) {
            return;
        }
        TrabajoIa trabajo = tomado.get();
        AgenteSeleccion agente = agentes.get(trabajo.getAgenteCodigo());
        if (agente == null) {
            registro.fallar(trabajoIaId, 0,
                    "No hay ningún agente que sepa atender " + trabajo.getAgenteCodigo());
            return;
        }

        try {
            agente.ejecutar(trabajo);
            registro.terminar(trabajoIaId);
            encolarSiguiente(trabajo);
        } catch (RuntimeException e) {
            // Nunca se guarda una nota inventada ni se mueve la postulación: solo se anota
            // el fallo y, si queda intento, se vuelve a poner en la cola.
            if (registro.fallar(trabajoIaId, maxIntentos, mensaje(e))) {
                publicador.publicar(trabajoIaId);
            }
        }
    }

    @Override
    public String comoVa(Long postulacionId) {
        List<TrabajoIa> suyos = trabajos.findByPostulacionIdOrderByIdAsc(postulacionId);
        if (suyos.isEmpty()) {
            return "SIN_EMPEZAR";
        }
        // Un trabajo vivo manda sobre todo lo demás: mientras quede uno, la calificación no
        // ha terminado, aunque los anteriores hayan salido bien.
        boolean vivo = suyos.stream()
                .anyMatch(t -> "PENDIENTE".equals(t.getEstado()) || "EN_CURSO".equals(t.getEstado()));
        if (vivo) {
            return "EN_CURSO";
        }
        if (suyos.stream().anyMatch(t -> "FALLIDO".equals(t.getEstado()))) {
            return "FALLIDA";
        }
        // Los tres acabaron. Que el retrato exista lo comprueba quien pregunta: aquí solo se
        // sabe de trabajos.
        return suyos.size() >= ORDEN.size() ? "TERMINADA" : "EN_CURSO";
    }

    @Override
    public void reintentarAtascados() {
        if (!habilitada) {
            return;
        }
        Instant limite = Instant.now().minus(limiteColgado);

        // Pendientes que llevan demasiado ahí: el mensaje se perdió, o RabbitMQ estaba caído
        // cuando se publicó. Volver a publicarlos es barato y el trabajo es idempotente.
        for (TrabajoIa trabajo : trabajos.findByEstadoAndCreadoEnBefore("PENDIENTE", limite)) {
            log.warn("El trabajo {} lleva pendiente desde {}: se vuelve a encolar",
                    trabajo.getId(), trabajo.getCreadoEn());
            publicador.publicar(trabajo.getId());
        }

        // En curso colgados: alguien lo tomó y no volvió. Pasa si el proceso murió a mitad.
        for (TrabajoIa trabajo : trabajos.findByEstadoAndTomadoEnBefore("EN_CURSO", limite)) {
            log.warn("El trabajo {} lleva en curso desde {} sin terminar: se devuelve a la cola",
                    trabajo.getId(), trabajo.getTomadoEn());
            registro.devolverAPendiente(trabajo.getId());
            publicador.publicar(trabajo.getId());
        }
    }

    /**
     * El siguiente de la fila.
     *
     * <p>Si el que acaba de terminar era el último, no hay nada que encolar: la postulación
     * ya está en {@code PERFIL_POR_CONFIRMAR}, esperando a una persona.
     */
    private void encolarSiguiente(TrabajoIa terminado) {
        int posicion = ORDEN.indexOf(terminado.getAgenteCodigo());
        if (posicion < 0 || posicion == ORDEN.size() - 1) {
            return;
        }
        encolar(terminado.getPostulacionId(), ORDEN.get(posicion + 1));
    }

    private void encolar(Long postulacionId, String agenteCodigo) {
        Long organizacionId = puente.organizacionDe(postulacionId);
        registro.crearSiHaceFalta(organizacionId, postulacionId, agenteCodigo)
                .ifPresent(trabajo -> publicador.publicar(trabajo.getId()));
    }

    private String mensaje(Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
