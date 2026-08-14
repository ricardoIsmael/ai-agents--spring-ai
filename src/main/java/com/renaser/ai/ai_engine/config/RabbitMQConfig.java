package com.renaser.ai.ai_engine.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String AGENT_EXCHANGE = "agent.exchange";
    public static final String AGENT_HANDOFF_QUEUE = "agent.handoff.queue";
    public static final String AGENT_HANDOFF_ROUTING_KEY = "agent.handoff.*";
    public static final String AGENT_EXECUTION_QUEUE = "agent.execution.queue";
    public static final String AGENT_EXECUTION_ROUTING_KEY = "agent.execute";

    @Bean
    public TopicExchange agentExchange() {
        return new TopicExchange(AGENT_EXCHANGE);
    }

    @Bean
    public Queue agentHandoffQueue() {
        return new Queue(AGENT_HANDOFF_QUEUE, true);
    }

    @Bean
    public Binding agentHandoffBinding(Queue agentHandoffQueue, TopicExchange agentExchange) {
        return BindingBuilder.bind(agentHandoffQueue).to(agentExchange).with(AGENT_HANDOFF_ROUTING_KEY);
    }

    @Bean
    public Queue agentExecutionQueue() {
        return new Queue(AGENT_EXECUTION_QUEUE, true);
    }

    @Bean
    public Binding agentExecutionBinding(Queue agentExecutionQueue, TopicExchange agentExchange) {
        return BindingBuilder.bind(agentExecutionQueue).to(agentExchange).with(AGENT_EXECUTION_ROUTING_KEY);
    }


    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
