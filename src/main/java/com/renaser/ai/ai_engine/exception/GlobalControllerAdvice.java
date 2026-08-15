package com.renaser.ai.ai_engine.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j //permite generar registros logs de forma profesionarl y automatica
public class GlobalControllerAdvice {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request){

        log.warn("Recurso no encontrado - Path: {}, Message: {}",request.getDescription(false),ex.getMessage());
        ProblemDetail problemDetail= ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

        problemDetail.setTitle("Recurso no encontrado");
        problemDetail.setType(URI.create("https://api.motoragentes.com/errors/not-found"));
        problemDetail.setProperty("Timestap", Instant.now());

        problemDetail.setProperty("Resource", ex.getResourceName());
        problemDetail.setProperty("Field", ex.getFieldName());
        problemDetail.setProperty("Value", ex.getFieldValue());

        return problemDetail;
    }
    @ExceptionHandler(ReviewNotAllowedException.class)
    public ProblemDetail handleReviewNotAllowedException(ReviewNotAllowedException ex, WebRequest request){

        log.warn("Reseña no permitida - Path: {}, Message: {}", request.getDescription(false), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());

        problemDetail.setTitle("Operación no permitida");
        problemDetail.setType(URI.create("https://api.motoragentes.com/errors/forbidden"));
        problemDetail.setProperty("Timestap", Instant.now());

        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex){


        ProblemDetail problemDetail= ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "La validacion fallo en uno o mas campos");

        problemDetail.setTitle("Error de Validacion");
        problemDetail.setType(URI.create("https://api.motoragentes.com/errors/error-validation"));
        problemDetail.setProperty("Timestap", Instant.now());

        Map<String,String> errorMap = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(
                e -> {
                    errorMap.put(e.getField(),e.getDefaultMessage());
                }
        );
        problemDetail.setProperty("errors",errorMap);
        return problemDetail;
    }


    // Una regla de negocio incumplida es un 400 con explicación, no un 500 opaco:
    // «toda transición manual exige motivo», «el archivo debe ser PDF o Word»…
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleReglaDeNegocio(IllegalArgumentException ex, WebRequest request) {
        log.warn("Regla de negocio - Path: {}, Message: {}", request.getDescription(false), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problemDetail.setTitle("La petición no cumple una regla");
        problemDetail.setType(URI.create("https://api.renaser.com/errors/regla-de-negocio"));
        problemDetail.setProperty("Timestap", Instant.now());
        return problemDetail;
    }

    // El estado actual no permite la operación: 409, con el porqué
    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleEstadoInvalido(IllegalStateException ex, WebRequest request) {
        log.warn("Estado no lo permite - Path: {}, Message: {}", request.getDescription(false), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("El estado actual no permite esta operación");
        problemDetail.setType(URI.create("https://api.renaser.com/errors/estado-invalido"));
        problemDetail.setProperty("Timestap", Instant.now());
        return problemDetail;
    }

    // Sin este handler, el genérico de abajo convertiría un «no puedes» (403) en un 500.
    // El doc de roles exige un mensaje claro cuando falta un permiso, no un error opaco.
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(org.springframework.security.access.AccessDeniedException ex,
                                            WebRequest request) {
        log.warn("Permiso denegado - Path: {}, Message: {}", request.getDescription(false), ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "No tienes permiso para hacer esto. Si crees que deberías, pide el acceso al administrador.");
        problemDetail.setTitle("Permiso denegado");
        problemDetail.setType(URI.create("https://api.renaser.com/errors/permiso-denegado"));
        problemDetail.setProperty("Timestap", Instant.now());
        return problemDetail;
    }

    // El CV tiene tope de 10 MB: el error debe decirlo, no responder un 500 mudo
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUpload(org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "El archivo supera el tamaño máximo permitido (10 MB)");
        problemDetail.setTitle("Archivo demasiado grande");
        problemDetail.setType(URI.create("https://api.renaser.com/errors/archivo-grande"));
        problemDetail.setProperty("Timestap", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handelException(Exception ex, WebRequest request){


        log.warn("A ocurrido un erro inesperado {}: {}",
                request.getDescription(false),ex.getMessage(),ex);
        ProblemDetail problemDetail= ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "A ocurrido un error inesaperado. Por favor contactar, con el administrador");

        problemDetail.setTitle("Interal Server Error");
        problemDetail.setType(URI.create("https://api.motoragentes.com/errors/internal"));
        problemDetail.setProperty("Timestap", Instant.now());

        return problemDetail;
    }

}
