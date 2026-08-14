package com.renaser.ai.ai_engine.mapper;

import com.renaser.ai.ai_engine.dto.AgentRunRequest;
import com.renaser.ai.ai_engine.dto.AgentRunResponse;
import com.renaser.ai.ai_engine.model.AgentRun;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AgentRunMapper {

    AgentRun toAgenteRun(AgentRunRequest request);
    AgentRunResponse toAgenteRunResponse(AgentRun entity);
}
