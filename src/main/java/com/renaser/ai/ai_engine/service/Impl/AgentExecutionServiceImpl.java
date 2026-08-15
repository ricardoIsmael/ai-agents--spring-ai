package com.renaser.ai.ai_engine.service.impl;

import com.renaser.ai.ai_engine.context.AgentContextResolver;
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
import com.renaser.ai.ai_engine.prompt.AgentModelSelector;
import com.renaser.ai.ai_engine.prompt.AgentPromptProvider;
import com.renaser.ai.ai_engine.repository.AgentRunRepository;
import com.renaser.ai.ai_engine.service.AgentExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentExecutionServiceImpl implements AgentExecutionService {

    private static final String CONTRACT_VERSION = "v2";

    private final ChatClient chatClient;
    private final AgentPromptProvider agentPromptProvider;
    private final AgentModelSelector agentModelSelector;
    private final AgentResponseTypeRegistry agentResponseTypeRegistry;
    private final AgentContextResolver agentContextResolver;
    private final AgentRunRepository agentRunRepository;
    private final AgentRunMapper agentRunMapper;
    private final AgentHandoffPublisher agentHandoffPublisher;
    private final AgentExecutionRequestPublisher agentExecutionRequestPublisher;
    private final JsonMapper jsonMapper;

    /**
     * Sin @Transactional a propósito: askAgent() puede tardar minutos y una transacción
     * abierta durante todo ese tiempo retiene una conexión del pool. Con el pool por defecto
     * (10 conexiones), una decena de corridas simultáneas congelaría la aplicación entera.
     * Cada save() es atómico por sí mismo, que es la única garantía que este flujo necesita.
     */
    @Override
    public AgentRunResponse execute(AgentRunRequest request) {
        UUID flowId = UUID.randomUUID();
        AgentResponse<?> aiResult = askAgent(request);

        AgentRun run = agentRunMapper.toAgenteRun(request);
        run.setFlowId(flowId);
        run.setCreatedAt(Instant.now());
        applyResult(run, aiResult);
        AgentRun saved = agentRunRepository.save(run);

        agentHandoffPublisher.publishFanOut(
                saved.getId(), flowId, saved.getEntityId(), saved.getObjective(), aiResult.routing(), 0, 1);

        return agentRunMapper.toAgenteRunResponse(saved);
    }

    @Override
    public UUID startFlow(AgentRunRequest request) {
        UUID flowId = UUID.randomUUID();
        enqueue(request, flowId, null, 0, 1);
        return flowId;
    }

    @Override
    public UUID enqueue(AgentRunRequest request) {
        return enqueue(request, UUID.randomUUID(), null, 0, 1);
    }

    @Override
    @Transactional
    public UUID enqueue(AgentRunRequest request, UUID flowId, UUID parentRunId, int depth, int totalRuns) {
        AgentRun pending = agentRunMapper.toAgenteRun(request);
        pending.setFlowId(flowId);
        pending.setParentRunId(parentRunId);
        pending.setDepth(depth);
        pending.setCreatedAt(Instant.now());
        AgentRun saved = agentRunRepository.save(pending);

        agentExecutionRequestPublisher.publishExecutionRequest(new AgentExecutionMessage(
                saved.getId(), flowId, parentRunId, request.agentType(),
                request.entityId(), request.objective(), depth, totalRuns));

        return saved.getId();
    }

    /**
     * Una corrida que revienta (modelo caído, JSON irrecuperable) debe quedar marcada como
     * fallida y visible en la traza. Sin este catch quedaba pendiente para siempre, y desde
     * fuera era indistinguible de "todavía procesando".
     *
     * Tampoco lleva @Transactional, por el mismo motivo que execute(): la llamada al modelo
     * nunca debe ocurrir con una transacción abierta.
     */
    @Override
    public void completeExecution(AgentExecutionMessage message) {
        AgentRun run = agentRunRepository.findById(message.runId())
                .orElseThrow(() -> new ResourceNotFoundException("AgentRun", "id", message.runId()));

        AgentRunRequest request = new AgentRunRequest(
                message.agentType(), message.entityId(), message.objective());

        AgentResponse<?> aiResult;
        try {
            aiResult = askAgent(request);
        } catch (RuntimeException e) {
            log.error("La corrida {} ({}) falló: {}", message.runId(), message.agentType(), e.getMessage());
            run.setErrorMessage(e.getClass().getSimpleName() + ": " + e.getMessage());
            run.setFinishedAt(Instant.now());
            agentRunRepository.save(run);
            return;
        }

        applyResult(run, aiResult);
        AgentRun saved = agentRunRepository.save(run);

        agentHandoffPublisher.publishFanOut(saved.getId(), message.flowId(), saved.getEntityId(),
                saved.getObjective(), aiResult.routing(), message.depth(), message.totalRuns());
    }

    private AgentResponse<?> askAgent(AgentRunRequest request) {
        String systemPrompt = agentPromptProvider.getSystemPrompt(request.agentType());
        String model = agentModelSelector.selectModel(request.agentType());
        ParameterizedTypeReference<?> responseType = agentResponseTypeRegistry.resolve(request.agentType());

        Object result = chatClient.prompt()
                .system(systemPrompt)
                .user(agentContextResolver.buildUserMessage(request))
                .options(OllamaChatOptions.builder().model(model).disableThinking())
                .call()
                .entity(responseType, spec -> spec.useProviderStructuredOutput().validateSchema());

        return (AgentResponse<?>) result;
    }

    private void applyResult(AgentRun run, AgentResponse<?> aiResult) {
        run.setVersion(CONTRACT_VERSION);
        run.setOutputJson(writeJson(aiResult));
        run.setSeverity(aiResult.severity() != null ? aiResult.severity().name() : null);
        run.setRequiresHumanApproval(aiResult.humanGate() != null && aiResult.humanGate().required());
        run.setFinishedAt(Instant.now());
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
