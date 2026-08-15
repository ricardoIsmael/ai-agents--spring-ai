package com.renaser.ai.ai_engine.prompt.impl;

import com.renaser.ai.ai_engine.model.AgentType;
import com.renaser.ai.ai_engine.prompt.AgentModelSelector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AgentModelSelectorImpl implements AgentModelSelector {

    // Punto de extensión para abaratar agentes: deepseek-v4-flash cuesta ~3x menos que
    // deepseek-v4-pro por token. Se puebla recién con datos de la tasa de parseo del
    // envelope al primer intento por agente, no por intuición — el precedente es qwen3:0.6b,
    // que parecía razonable y descarrilaba en Orchestrator (routing con juicio real) y en
    // Narrative Message (contexto de avisos por encima de su ventana, JSON truncado y
    // cuelgues reintentando).
    private static final Map<AgentType, String> LIGHT_MODEL_OVERRIDES = Map.of();

    private final String defaultModel;

    public AgentModelSelectorImpl(@Value("${renaser.ai.chat.default-model}") String defaultModel) {
        this.defaultModel = defaultModel;
    }

    @Override
    public String selectModel(AgentType agentType) {
        return LIGHT_MODEL_OVERRIDES.getOrDefault(agentType, defaultModel);
    }
}
