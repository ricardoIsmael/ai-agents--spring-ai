package com.renaser.ai.ai_engine.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent_run")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class AgentRun
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private AgentType agentType;

    private String entityId;
    private String objective;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String outputJson;

    // info|attention|risk|critical — viene del envelope, null mientras el run está pendiente
    private String severity;

    // derivado de humanGate.required() del envelope (Human Gate es por acción, no por agente,
    // pero a nivel de fila seguimos necesitando saber si ESTE run tiene algo pendiente de aprobar)
    private boolean requiresHumanApproval;
    private boolean approved;

    // nextAgent (columna única) ya no existe: el V2 usa routing[] con varios destinos posibles
    // en paralelo. El fan-out se lee directo del outputJson al momento del handoff, no se
    // persiste como columna — ver AgentExecutionServiceImpl.

    private Instant createdAt;
    private String version;
}
