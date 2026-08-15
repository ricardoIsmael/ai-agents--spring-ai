package com.renaser.ai.ai_engine.context.impl;

import com.renaser.ai.ai_engine.context.AgentContextProvider;
import com.renaser.ai.ai_engine.dto.AgentRunRequest;
import com.renaser.ai.ai_engine.model.AgentType;
import com.renaser.ai.ai_engine.supabase.CobroRecord;
import com.renaser.ai.ai_engine.supabase.SupabaseDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CobrosContextProvider implements AgentContextProvider {

    private final SupabaseDataService supabaseDataService;

    @Override
    public AgentType agentType() {
        return AgentType.COLLECTIONS;
    }

    @Override
    public String contextHeader() {
        return "Cobros del cliente (datos reales, evidence_id = id del cobro):";
    }

    // El entityId del request identifica al cliente (columna "cliente" de la tabla cobros).
    @Override
    public String buildContext(AgentRunRequest request) {
        return supabaseDataService.getCobrosByCliente(request.entityId()).stream()
                .map(this::describe)
                .collect(Collectors.joining("\n"));
    }

    private String describe(CobroRecord c) {
        return "[evidence_id=%s] %s: total %s, pagado %s, pendiente %s, cuota %s/%s, vence %s, estado %s".formatted(
                c.id(), c.concepto(), c.total(), c.pagado(), c.pendiente(),
                c.cuotaActual(), c.cuotas(), c.vence(), c.estado());
    }
}
