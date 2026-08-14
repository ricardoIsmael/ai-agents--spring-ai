package com.renaser.ai.ai_engine.service.impl;

import com.renaser.ai.ai_engine.dto.AgentResponse;
import com.renaser.ai.ai_engine.dto.AgentResponseTypeRegistry;
import com.renaser.ai.ai_engine.dto.AgentRunRequest;
import com.renaser.ai.ai_engine.dto.AgentRunResponse;
import com.renaser.ai.ai_engine.exception.ResourceNotFoundException;
import com.renaser.ai.ai_engine.mapper.AgentRunMapper;
import com.renaser.ai.ai_engine.messaging.AgentExecutionMessage;
import com.renaser.ai.ai_engine.messaging.AgentExecutionRequestPublisher;
import com.renaser.ai.ai_engine.messaging.AgentHandoffPublisher;
import com.renaser.ai.ai_engine.model.AgentRun;
import com.renaser.ai.ai_engine.model.AgentType;
import com.renaser.ai.ai_engine.prompt.AgentModelSelector;
import com.renaser.ai.ai_engine.prompt.AgentPromptProvider;
import com.renaser.ai.ai_engine.rag.DocumentRetrievalService;
import com.renaser.ai.ai_engine.rag.SearchResultResponse;
import com.renaser.ai.ai_engine.repository.AgentRunRepository;
import com.renaser.ai.ai_engine.service.AgentExecutionService;
import com.renaser.ai.ai_engine.supabase.ActividadRecord;
import com.renaser.ai.ai_engine.supabase.AvisoRecord;
import com.renaser.ai.ai_engine.supabase.CobroRecord;
import com.renaser.ai.ai_engine.supabase.EntregableRecord;
import com.renaser.ai.ai_engine.supabase.EventoRecord;
import com.renaser.ai.ai_engine.supabase.MotorRecord;
import com.renaser.ai.ai_engine.supabase.ProspectoRecord;
import com.renaser.ai.ai_engine.supabase.SupabaseDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgentExecutionServiceImpl implements AgentExecutionService {

    private static final String CONTRACT_VERSION = "v2";

    private final ChatClient chatClient;
    private final AgentPromptProvider agentPromptProvider;
    private final AgentModelSelector agentModelSelector;
    private final AgentResponseTypeRegistry agentResponseTypeRegistry;
    private final AgentRunRepository agentRunRepository;
    private final AgentRunMapper agentRunMapper;
    private final AgentHandoffPublisher agentHandoffPublisher;
    private final AgentExecutionRequestPublisher agentExecutionRequestPublisher;
    private final DocumentRetrievalService documentRetrievalService;
    private final SupabaseDataService supabaseDataService;
    private final JsonMapper jsonMapper;

    @Override
    public AgentRunResponse execute(AgentRunRequest request) {
        AgentResponse<?> aiResult = askAgent(request);
        AgentRun saved = agentRunRepository.save(buildRun(request, aiResult));

        agentHandoffPublisher.publishFanOut(
                saved.getId(), saved.getEntityId(), saved.getObjective(), aiResult.routing(), 0, 1);

        return agentRunMapper.toAgenteRunResponse(saved);
    }

    @Override
    public UUID enqueue(AgentRunRequest request) {
        return enqueue(request, 0, 1);
    }

    @Override
    public UUID enqueue(AgentRunRequest request, int depth, int totalRuns) {
        AgentRun pending = agentRunMapper.toAgenteRun(request);
        pending.setCreatedAt(Instant.now());
        AgentRun saved = agentRunRepository.save(pending);

        agentExecutionRequestPublisher.publishExecutionRequest(new AgentExecutionMessage(
                saved.getId(), request.agentType(), request.entityId(), request.objective(), depth, totalRuns));

        return saved.getId();
    }

    @Override
    public void completeExecution(AgentExecutionMessage message) {
        AgentRunRequest request = new AgentRunRequest(message.agentType(), message.entityId(), message.objective());
        AgentResponse<?> aiResult = askAgent(request);

        AgentRun run = agentRunRepository.findById(message.runId())
                .orElseThrow(() -> new ResourceNotFoundException("AgentRun", "id", message.runId()));

        applyResult(run, aiResult);
        AgentRun saved = agentRunRepository.save(run);

        agentHandoffPublisher.publishFanOut(saved.getId(), saved.getEntityId(), saved.getObjective(),
                aiResult.routing(), message.depth(), message.totalRuns());
    }

    private AgentResponse<?> askAgent(AgentRunRequest request) {
        String systemPrompt = agentPromptProvider.getSystemPrompt(request.agentType());
        String model = agentModelSelector.selectModel(request.agentType());
        ParameterizedTypeReference<?> responseType = agentResponseTypeRegistry.resolve(request.agentType());

        Object result = chatClient.prompt()
                .system(systemPrompt)
                .user(buildUserMessage(request))
                .options(OllamaChatOptions.builder().model(model).disableThinking())
                .call()
                .entity(responseType, spec -> spec.useProviderStructuredOutput().validateSchema());

        return (AgentResponse<?>) result;
    }

    // Agentes con contexto externo conectado a datos reales de Supabase/RAG. FINANCE
    // (movimientos/cuentas) y TALENT_INTELLIGENCE (evaluaciones_360/planes_mejora) quedan
    // fuera a propósito: esas partes de la app siguen en desarrollo (pagos y postulaciones).
    // CEO (decisiones), CLIENT_SUCCESS (clientes_programa), QA_GOVERNANCE
    // (validaciones/indicaciones) y DIAGNOSTIC no tienen todavía ninguna tabla con filas
    // reales que conectar — quedan en objective puro hasta que exista data.
    private String buildUserMessage(AgentRunRequest request) {
        return switch (request.agentType()) {
            case KNOWLEDGE -> withKnowledgeContext(request);
            case CONSULTING -> withMotoresContext(request);
            case COLLECTIONS -> withCobrosContext(request);
            case OPERATIONS -> withActividadesContext(request);
            case GROWTH -> withProspectosContext(request);
            case EVENT -> withEventoContext(request);
            case AUDITOR -> withEntregablesContext(request);
            case NARRATIVE_MESSAGE -> withAvisosContext(request);
            default -> request.objective();
        };
    }

    private String withKnowledgeContext(AgentRunRequest request) {
        List<SearchResultResponse> results = documentRetrievalService.search(request.objective());
        if (results.isEmpty()) {
            return request.objective();
        }

        String context = results.stream()
                .map(SearchResultResponse::text)
                .collect(Collectors.joining("\n---\n"));

        return request.objective() + "\n\nContexto recuperado de la base de conocimiento (search_knowledge):\n" + context;
    }

    private String withMotoresContext(AgentRunRequest request) {
        List<MotorRecord> motores = supabaseDataService.getMotores();
        if (motores.isEmpty()) {
            return request.objective();
        }

        String context = motores.stream()
                .map(m -> "Motor %d - %s: %s = %s (meta %s), estado %s".formatted(
                        m.n(), m.nombre(), m.kpi(), m.valor(), m.meta(), m.estado()))
                .collect(Collectors.joining("\n"));

        return request.objective() + "\n\nContexto de los 8 motores estratégicos (datos reales de Supabase):\n" + context;
    }

    // El entityId del request identifica al cliente (columna "cliente" en la tabla cobros).
    private String withCobrosContext(AgentRunRequest request) {
        List<CobroRecord> cobros = supabaseDataService.getCobrosByCliente(request.entityId());
        if (cobros.isEmpty()) {
            return request.objective();
        }

        String context = cobros.stream()
                .map(c -> "Cobro %s - %s: total %s, pagado %s, pendiente %s, cuota %s/%s, vence %s, estado %s".formatted(
                        c.id(), c.concepto(), c.total(), c.pagado(), c.pendiente(),
                        c.cuotaActual(), c.cuotas(), c.vence(), c.estado()))
                .collect(Collectors.joining("\n"));

        return request.objective() + "\n\nContexto de cobros del cliente (datos reales de Supabase):\n" + context;
    }

    private String withActividadesContext(AgentRunRequest request) {
        List<ActividadRecord> actividades = supabaseDataService.getActividadesBloqueadas();
        if (actividades.isEmpty()) {
            return request.objective();
        }

        String context = actividades.stream()
                .map(a -> "\"%s\" (%s, prioridad %s, avance %d%%): %s".formatted(
                        a.titulo(), a.estado(), a.prioridad(), a.avance(), a.bloqueo()))
                .collect(Collectors.joining("\n"));

        return request.objective() + "\n\nActividades bloqueadas en la empresa (datos reales de Supabase):\n" + context;
    }

    private String withProspectosContext(AgentRunRequest request) {
        List<ProspectoRecord> prospectos = supabaseDataService.getProspectos();
        if (prospectos.isEmpty()) {
            return request.objective();
        }

        String context = prospectos.stream()
                .map(p -> "%s - etapa %s, origen %s, valor %s, responsable %s".formatted(
                        p.nombre(), p.etapa(), p.origen(), p.valor(), p.responsable()))
                .collect(Collectors.joining("\n"));

        return request.objective() + "\n\nEmbudo comercial actual (datos reales de Supabase):\n" + context;
    }

    // El entityId del request identifica al evento (columna "nombre" en la tabla eventos).
    private String withEventoContext(AgentRunRequest request) {
        List<EventoRecord> eventos = supabaseDataService.getEventoByNombre(request.entityId());
        if (eventos.isEmpty()) {
            return request.objective();
        }

        String context = eventos.stream()
                .map(e -> "%s: ingresos %s, egresos %s".formatted(e.nombre(), e.ingresos(), e.egresos()))
                .collect(Collectors.joining("\n"));

        return request.objective() + "\n\nContexto del evento (datos reales de Supabase):\n" + context;
    }

    private String withEntregablesContext(AgentRunRequest request) {
        List<EntregableRecord> entregables = supabaseDataService.getEntregablesPendientes();
        if (entregables.isEmpty()) {
            return request.objective();
        }

        String context = entregables.stream()
                .map(e -> "\"%s\" (%s, v%s) pendiente de revisión desde %s".formatted(
                        e.nombre(), e.tipo(), e.version(), e.createdAt()))
                .collect(Collectors.joining("\n"));

        return request.objective() + "\n\nEntregables pendientes de revisión (datos reales de Supabase):\n" + context;
    }

    private String withAvisosContext(AgentRunRequest request) {
        List<AvisoRecord> avisos = supabaseDataService.getAvisosActivos();
        if (avisos.isEmpty()) {
            return request.objective();
        }

        String context = avisos.stream()
                .map(a -> "[%s] %s (área %s, responsable %s): %s".formatted(
                        a.sev(), a.titulo(), a.area(), a.responsable(), a.detalle()))
                .collect(Collectors.joining("\n"));

        return request.objective() + "\n\nAvisos activos sin leer (datos reales de Supabase):\n" + context;
    }

    private AgentRun buildRun(AgentRunRequest request, AgentResponse<?> aiResult) {
        AgentRun run = agentRunMapper.toAgenteRun(request);
        run.setCreatedAt(Instant.now());
        applyResult(run, aiResult);
        return run;
    }

    private void applyResult(AgentRun run, AgentResponse<?> aiResult) {
        run.setVersion(CONTRACT_VERSION);
        run.setOutputJson(writeJson(aiResult));
        run.setSeverity(aiResult.severity() != null ? aiResult.severity().name() : null);
        run.setRequiresHumanApproval(aiResult.humanGate() != null && aiResult.humanGate().required());
    }

    private String writeJson(AgentResponse<?> aiResult) {
        // Jackson 3: JacksonException ya es unchecked, este catch solo agrega un mensaje de dominio
        try {
            return jsonMapper.writeValueAsString(aiResult);
        } catch (JacksonException e) {
            throw new IllegalStateException("No se pudo serializar la respuesta del agente", e);
        }
    }
}
