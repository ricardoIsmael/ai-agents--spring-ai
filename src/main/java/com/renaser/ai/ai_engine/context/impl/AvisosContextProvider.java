package com.renaser.ai.ai_engine.context.impl;

import com.renaser.ai.ai_engine.context.AgentContextProvider;
import com.renaser.ai.ai_engine.dto.AgentRunRequest;
import com.renaser.ai.ai_engine.model.AgentType;
import com.renaser.ai.ai_engine.supabase.AvisoDataProvider;
import com.renaser.ai.ai_engine.supabase.AvisoRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AvisosContextProvider implements AgentContextProvider {

    private final AvisoDataProvider avisoDataProvider;

    @Override
    public AgentType agentType() {
        return AgentType.NARRATIVE_MESSAGE;
    }

    @Override
    public String contextHeader() {
        return "Avisos activos sin leer, más recientes primero (datos reales):";
    }

    @Override
    public String buildContext(AgentRunRequest request) {
        return avisoDataProvider.getAvisosActivos().stream()
                .map(this::describe)
                .collect(Collectors.joining("\n"));
    }

    private String describe(AvisoRecord a) {
        return "[%s] %s (área %s, responsable %s): %s".formatted(
                a.sev(), a.titulo(), a.area(), a.responsable(), a.detalle());
    }
}
