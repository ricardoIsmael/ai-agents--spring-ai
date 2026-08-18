package com.renaser.ai.ai_engine.ai.service;

/**
 * La cola que hace que una postulación se califique sola.
 *
 * <p>Cuando el candidato entrega su evaluación, el código puntúa lo cerrado al momento y la
 * postulación queda en {@code PERFIL_CALIFICANDO}. Lo que falta —leer el currículum,
 * calificar lo abierto y armar el Perfil de Talento— tarda decenas de segundos y depende de
 * un servicio externo, así que no puede hacerse dentro de la petición del candidato. De eso
 * se encarga esto.
 *
 * <p><b>Los tres agentes corren en fila, no a la vez.</b> El Perfil de Talento necesita lo
 * que dejaron los otros dos, así que igual habría que esperarlos; y en fila hay un solo
 * trabajo vivo por postulación, que es lo que hace que reintentar sea trivial en vez de
 * tener que decidir qué hacer cuando uno de dos paralelos falla.
 *
 * <p><b>Si la IA falla se reintenta y nunca se inventa una nota</b> (Regla 3 del doc 03). La
 * postulación se queda en {@code PERFIL_CALIFICANDO} hasta que haya resultado de verdad.
 */
public interface ColaCalificacionIa {

    /**
     * Arranca la calificación de una postulación que acaba de entregar su evaluación.
     *
     * <p>Es idempotente: llamarla dos veces no duplica trabajos ni recalifica lo ya hecho.
     */
    void encolarPerfilIntegral(Long postulacionId);

    /**
     * Ejecuta un trabajo concreto. Lo llama el listener de la cola, y también el sondeo.
     *
     * <p>No lanza excepción: el resultado —bien, a reintentar o fallido— queda escrito en
     * {@code trabajo_ia} y en {@code ejecucion_ia}.
     */
    void ejecutar(Long trabajoIaId);

    /**
     * Vuelve a empujar lo que se quedó atascado: mensajes que se perdieron y trabajos que
     * alguien tomó y no terminó porque el proceso murió a mitad.
     */
    void reintentarAtascados();

    /**
     * En qué punto va la calificación de una postulación, según sus trabajos.
     *
     * <p>Existe para que quien pregunte no tenga que mirar {@code trabajo_ia} por su cuenta:
     * la cola es la única que sabe cuántos agentes van en fila y cuál toca ahora.
     *
     * <p><b>El estado de la postulación no sirve como señal.</b> Solo pasa a
     * {@code PERFIL_CALIFICANDO} cuando el candidato entrega su evaluación, así que una
     * calificación pedida desde el panel corre sin que ese estado cambie: preguntarle a la
     * postulación diría «no hay nada» mientras los tres agentes están trabajando.
     *
     * @return {@code EN_CURSO} si queda algún trabajo vivo, {@code FALLIDA} si el último se
     *         agotó en reintentos, {@code TERMINADA} si los tres acabaron, o
     *         {@code SIN_EMPEZAR} si nadie ha pedido nada todavía.
     */
    String comoVa(Long postulacionId);
}
