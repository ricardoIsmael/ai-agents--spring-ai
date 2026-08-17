package com.renaser.ai.ai_engine.ai.prompt;

import com.renaser.ai.ai_engine.ai.model.AgentType;

public interface AgentModelSelector {

    // Devuelve el modelo de Ollama que debe usar cada agente (livianos usan un modelo más chico)
    String selectModel(AgentType agentType);
}
