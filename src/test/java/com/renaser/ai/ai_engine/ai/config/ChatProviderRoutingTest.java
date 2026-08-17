package com.renaser.ai.ai_engine.ai.config;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaApiAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaEmbeddingAutoConfiguration;
import org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El motor deja los dos starters en el classpath a propósito: DeepSeek sirve el chat y
 * Ollama se conserva solo por los embeddings, porque DeepSeek no expone ese endpoint.
 * <p>
 * Ese arreglo depende enteramente de spring.ai.model.chat y spring.ai.model.embedding. Si
 * alguien los borra o los invierte, el síntoma no es un error de arranque claro: el chat se
 * va callado a gemma4 local y vuelven las corridas de 70-90 s que motivaron la migración.
 * Este test fija ese contrato.
 * <p>
 * Usa ApplicationContextRunner en vez de @SpringBootTest para no arrastrar JPA ni pgvector,
 * que necesitan un Postgres vivo.
 */
class ChatProviderRoutingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RestClientAutoConfiguration.class,
                    ToolCallingAutoConfiguration.class,
                    DeepSeekChatAutoConfiguration.class,
                    OllamaApiAutoConfiguration.class,
                    OllamaChatAutoConfiguration.class,
                    OllamaEmbeddingAutoConfiguration.class))
            .withPropertyValues(
                    "spring.ai.model.chat=deepseek",
                    "spring.ai.model.embedding=ollama",
                    "spring.ai.deepseek.api-key=test-key",
                    "spring.ai.deepseek.chat.options.model=deepseek-v4-pro",
                    "spring.ai.ollama.base-url=http://localhost:11434",
                    "spring.ai.ollama.embedding.options.model=qwen3-embedding:0.6b",
                    "spring.ai.ollama.init.pull-model-strategy=never");

    @Test
    void elChatLoSirveDeepSeekYNoOllama() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ChatModel.class);
            assertThat(context).getBean(ChatModel.class).isInstanceOf(DeepSeekChatModel.class);
        });
    }

    @Test
    void losEmbeddingsSiguenEnOllama() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(EmbeddingModel.class);
            assertThat(context).getBean(EmbeddingModel.class).isInstanceOf(OllamaEmbeddingModel.class);
        });
    }
}
