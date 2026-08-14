package com.renaser.ai.ai_engine.prompt.impl;

import com.renaser.ai.ai_engine.model.AgentType;
import com.renaser.ai.ai_engine.prompt.AgentModelSelector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AgentModelSelectorImpl implements AgentModelSelector {

    // qwen3:0.6b probado y descartado para dos agentes: Orchestrator (routing con juicio
    // real, no clasificación trivial) y Narrative Message (su contexto real de avisos supera
    // la ventana de contexto de 4096 tokens del modelo, produce JSON truncado y cuelga
    // reintentando). Ambos usan el modelo por defecto hasta encontrar un modelo liviano que
    // sostenga JSON estructurado con contexto real inyectado.
    private static final Map<AgentType, String> LIGHT_MODEL_OVERRIDES = Map.of();

    private final String defaultModel;

    public AgentModelSelectorImpl(@Value("${spring.ai.ollama.chat.options.model}") String defaultModel) {
        this.defaultModel = defaultModel;
    }

    @Override
    public String selectModel(AgentType agentType) {
        return LIGHT_MODEL_OVERRIDES.getOrDefault(agentType, defaultModel);
    }
}
