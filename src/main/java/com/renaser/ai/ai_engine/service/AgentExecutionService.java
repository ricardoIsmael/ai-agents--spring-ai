package com.renaser.ai.ai_engine.service;

import com.renaser.ai.ai_engine.dto.AgentRunRequest;
import com.renaser.ai.ai_engine.dto.AgentRunResponse;
import com.renaser.ai.ai_engine.messaging.AgentExecutionMessage;

import java.util.UUID;

public interface AgentExecutionService {

    // Ejecuta un agente contra el modelo de IA de forma síncrona: el llamador espera la respuesta completa
    AgentRunResponse execute(AgentRunRequest request);

    // Encola un caso nuevo (depth=0, totalRuns=1) y devuelve de inmediato el id del run (pendiente)
    UUID enqueue(AgentRunRequest request);

    // Encola la continuación de una cadena existente (handoff), con su depth/totalRuns ya avanzados
    UUID enqueue(AgentRunRequest request, int depth, int totalRuns);

    // Llamado por el listener de la cola: ejecuta el agente y completa el run ya creado por enqueue()
    void completeExecution(AgentExecutionMessage message);
}
