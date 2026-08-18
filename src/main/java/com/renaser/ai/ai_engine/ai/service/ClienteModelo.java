package com.renaser.ai.ai_engine.ai.service;

import com.renaser.ai.ai_engine.ai.dto.RespuestaModelo;

/**
 * Una llamada al modelo, y nada más.
 *
 * <p><b>Por qué es una interfaz de un solo método.</b> Todo lo demás —la bitácora, los
 * reintentos, la cola, qué se guarda— se puede probar sin gastar un centavo si esta pieza se
 * puede sustituir por un doble. Sin ella, cualquier prueba del camino completo tendría que
 * llamar de verdad a DeepSeek, y eso significa pruebas lentas, caras y distintas cada vez.
 *
 * <p>También es lo que permite mover el chat a un modelo local sin tocar a los tres agentes:
 * hoy el candidato se lee en un servicio externo y esa decisión sigue abierta.
 */
public interface ClienteModelo {

    /**
     * @param agenteCodigo cuál de los agentes pregunta; permite elegir modelo por agente
     * @param instruccion  el texto que Dirección administra desde el panel
     * @param contenido    los datos del candidato, ya en JSON
     */
    RespuestaModelo preguntar(String agenteCodigo, String instruccion, String contenido);
}
