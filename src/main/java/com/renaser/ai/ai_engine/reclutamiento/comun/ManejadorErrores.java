package com.renaser.ai.ai_engine.reclutamiento.comun;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;

/**
 * Los errores propios del módulo de reclutamiento.
 *
 * <p>Vive aquí y no en el advice general por dos razones: el advice general es del módulo de
 * agentes y lo mantiene otra persona, y su handler de {@code Exception} convertiría estos casos
 * en un 500 opaco. Con {@code basePackages} solo actúa sobre nuestros controladores, y con
 * {@code HIGHEST_PRECEDENCE} se consulta antes que el general.
 */
@RestControllerAdvice(basePackages = "com.renaser.ai.ai_engine.reclutamiento")
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class ManejadorErrores {

    private static final String BASE_URI = "https://api.renaser.com/errors/";

    // Una regla de negocio incumplida es un 400 con explicación, no un 500 opaco:
    // «toda transición manual exige motivo», «el archivo debe ser PDF o Word»…
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail reglaDeNegocio(IllegalArgumentException ex, WebRequest request) {
        log.warn("Regla de negocio - Path: {}, Message: {}", request.getDescription(false), ex.getMessage());
        return construir(HttpStatus.BAD_REQUEST, "La petición no cumple una regla",
                "regla-de-negocio", ex.getMessage());
    }

    // El estado actual no permite la operación: 409, con el porqué
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail estadoInvalido(IllegalStateException ex, WebRequest request) {
        log.warn("Estado no lo permite - Path: {}, Message: {}", request.getDescription(false), ex.getMessage());
        return construir(HttpStatus.CONFLICT, "El estado actual no permite esta operación",
                "estado-invalido", ex.getMessage());
    }

    // El documento de roles exige un mensaje claro cuando falta un permiso, no un error opaco
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail permisoDenegado(AccessDeniedException ex, WebRequest request) {
        log.warn("Permiso denegado - Path: {}, Message: {}", request.getDescription(false), ex.getMessage());
        return construir(HttpStatus.FORBIDDEN, "Permiso denegado", "permiso-denegado",
                "No tienes permiso para hacer esto. Si crees que deberías, pide el acceso al administrador.");
    }

    static ProblemDetail construir(HttpStatus estado, String titulo, String sufijoTipo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setType(URI.create(BASE_URI + sufijoTipo));
        problema.setProperty("Timestamp", Instant.now());
        return problema;
    }
}
