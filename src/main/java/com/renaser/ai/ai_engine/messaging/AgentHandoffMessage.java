package com.renaser.ai.ai_engine.messaging;

import com.renaser.ai.ai_engine.model.AgentType;

import java.util.UUID;

// Lo que viaja por RabbitMQ cuando un agente termina y le pasa la posta a otro.
// Un run con routing[] de varios destinos genera un AgentHandoffMessage por cada destino
// (fan-out) — cada uno es independiente y lleva su propio depth/totalRuns.
public record AgentHandoffMessage(
        UUID runId,
        String entityId,
        String objective,
        AgentType nextAgent,
        int depth,
        int totalRuns
) {
}
