package com.renaser.ai.ai_engine.context.impl;

import com.renaser.ai.ai_engine.context.AgentContextProvider;
import com.renaser.ai.ai_engine.dto.AgentRunRequest;
import com.renaser.ai.ai_engine.model.AgentType;
import com.renaser.ai.ai_engine.supabase.EventoRecord;
import com.renaser.ai.ai_engine.supabase.SupabaseDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventoContextProvider implements AgentContextProvider {

    private final SupabaseDataService supabaseDataService;

    @Override
    public AgentType agentType() {
        return AgentType.EVENT;
    }

    @Override
    public String contextHeader() {
        return "Datos del evento (reales, evidence_id = id del evento):";
    }

    // El entityId del request identifica al evento (columna "nombre" de la tabla eventos).
    @Override
    public String buildContext(AgentRunRequest request) {
        return supabaseDataService.getEventoByNombre(request.entityId()).stream()
                .map(this::describe)
                .collect(Collectors.joining("\n"));
    }

    private String describe(EventoRecord e) {
        return "[evidence_id=%s] %s: ingresos %s, egresos %s".formatted(
                e.id(), e.nombre(), e.ingresos(), e.egresos());
    }
}
