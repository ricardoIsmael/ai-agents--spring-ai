package com.renaser.ai.ai_engine.dto;

import com.renaser.ai.ai_engine.model.AgentType;
public record AgentRunRequest(
        AgentType agentType,
        String entityId,
        String objective
) {
}
