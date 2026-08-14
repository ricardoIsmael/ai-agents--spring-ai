package com.renaser.ai.ai_engine.messaging;

import com.renaser.ai.ai_engine.model.AgentType;

import java.util.UUID;

// Lo que viaja por RabbitMQ para pedirle al listener que ejecute un agente de forma asíncrona.
// runId ya existe en la base (creado por enqueue()) — el listener completa esa fila, no crea una nueva.
// depth/totalRuns acompañan la cadena completa para que AgentChainLimits pueda cortarla.
public record AgentExecutionMessage(
        UUID runId,
        AgentType agentType,
        String entityId,
        String objective,
        int depth,
        int totalRuns
) {
}
