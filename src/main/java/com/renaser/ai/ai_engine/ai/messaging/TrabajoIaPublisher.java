package com.renaser.ai.ai_engine.ai.messaging;

import com.renaser.ai.ai_engine.ai.config.RabbitMQConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrabajoIaPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publicar(Long trabajoIaId) {
        log.info("Encolando el trabajo de calificación {}", trabajoIaId);
        rabbitTemplate.convertAndSend(RabbitMQConfig.AGENT_EXCHANGE,
                RabbitMQConfig.SELECCION_CALIFICACION_ROUTING_KEY,
                new TrabajoIaMessage(trabajoIaId));
    }
}
