package com.renaser.ai.ai_engine.prompt;

import com.renaser.ai.ai_engine.model.AgentType;

public interface AgentPromptProvider {

    // Devuelve el system prompt completo de un agente: reglas base del sistema + rol específico
    String getSystemPrompt(AgentType agentType);
}
