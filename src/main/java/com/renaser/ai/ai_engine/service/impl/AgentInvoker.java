package com.renaser.ai.ai_engine.service.impl;

import com.renaser.ai.ai_engine.context.AgentContextResolver;
import com.renaser.ai.ai_engine.dto.AgentResponse;
import com.renaser.ai.ai_engine.dto.AgentResponseTypeRegistry;
import com.renaser.ai.ai_engine.dto.AgentRunRequest;
import com.renaser.ai.ai_engine.model.AgentRun;
import com.renaser.ai.ai_engine.prompt.AgentPromptProvider;
import com.renaser.ai.ai_engine.prompt.ChatOptionsFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

/**
 * Aísla la llamada al modelo (prompt + contexto + parseo tipado) y el volcado del resultado
 * sobre un AgentRun. Separado de AgentExecutionServiceImpl para que la orquestación del flujo
 * (guardar, encolar, publicar handoff) no dependa de los detalles de cómo se habla con el LLM.
 */
@Component
@RequiredArgsConstructor
public class AgentInvoker {

    private static final String CONTRACT_VERSION = "v2";

    private final ChatClient chatClient;
    private final AgentPromptProvider agentPromptProvider;
    private final ChatOptionsFactory chatOptionsFactory;
    private final AgentResponseTypeRegistry agentResponseTypeRegistry;
    private final AgentContextResolver agentContextResolver;
    private final JsonMapper jsonMapper;

    public AgentResponse<?> ask(AgentRunRequest request) {
        String systemPrompt = agentPromptProvider.getSystemPrompt(request.agentType());
        ParameterizedTypeReference<?> responseType = agentResponseTypeRegistry.resolve(request.agentType());

        // Sin useProviderStructuredOutput(): DeepSeek expone JSON mode (json_object) pero no
        // JSON Schema estricto, así que no hay structured output nativo del proveedor al que
        // delegar. El converter de Spring AI inyecta el schema en el prompt y auto-corrige
        // si la salida no parsea. La contracara es que el contrato deja de estar garantizado
        // por el proveedor y pasa a depender del prompt: por eso se mide la tasa de parseo
        // al primer intento antes de mover agentes a modelos más baratos.
        Object result = chatClient.prompt()
                .system(systemPrompt)
                .user(agentContextResolver.buildUserMessage(request))
                .options(chatOptionsFactory.forAgent(request.agentType()))
                .call()
                .entity(responseType);

        return (AgentResponse<?>) result;
    }

    public void applyResult(AgentRun run, AgentResponse<?> aiResult) {
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
