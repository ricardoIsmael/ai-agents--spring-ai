package com.renaser.ai.ai_engine.perfilintegral.service;

/**
 * La parte de la calificación que no necesita inteligencia artificial.
 *
 * <p>Existe separada a propósito. Las preguntas cerradas se puntúan contra una clave
 * versionada y las contradicciones se detectan comparando dos números: eso es aritmética, y
 * un modelo generativo no debe tocarlo (RF-147). Así además esta parte se puede construir y
 * probar entera sin depender de que la IA responda.
 */
public interface ServicioCalificacion {

    /**
     * Puntúa lo cerrado de una evaluación terminada y levanta las alertas de consistencia.
     *
     * @return la nota de la evaluación sobre 100
     */
    java.math.BigDecimal calificarLoCerrado(Long postulacionId);
}
