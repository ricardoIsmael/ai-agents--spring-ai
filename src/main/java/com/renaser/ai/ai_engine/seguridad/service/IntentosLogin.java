package com.renaser.ai.ai_engine.seguridad.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Bloqueo temporal tras varios intentos fallidos seguidos. BCrypt solo no frena la
// fuerza bruta. En memoria: si el proceso se reinicia, el contador vuelve a cero,
// lo cual es aceptable para este umbral.
@Component
public class IntentosLogin {

    private record Estado(int fallos, Instant bloqueadoHasta) {}

    private final Map<String, Estado> porCorreo = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String correo) {
        Estado estado = porCorreo.get(clave(correo));
        return estado != null && estado.bloqueadoHasta() != null
                && Instant.now().isBefore(estado.bloqueadoHasta());
    }

    public void registrarFallo(String correo, int maximo, int minutosBloqueo) {
        porCorreo.compute(clave(correo), (k, actual) -> {
            int fallos = (actual == null ? 0 : actual.fallos()) + 1;
            Instant hasta = fallos >= maximo
                    ? Instant.now().plusSeconds(minutosBloqueo * 60L)
                    : null;
            // Al bloquear, el contador se reinicia: cumplida la espera, vuelve a empezar
            return new Estado(hasta != null ? 0 : fallos, hasta);
        });
    }

    public void registrarExito(String correo) {
        porCorreo.remove(clave(correo));
    }

    private String clave(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase();
    }
}
