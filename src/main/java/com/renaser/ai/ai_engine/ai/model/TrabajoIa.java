package com.renaser.ai.ai_engine.ai.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

// La cola de trabajo del hito 2 de selección. Regla 3 del doc 03 (selección): si la IA
// falla se reintenta, nunca se inventa una nota. referenciaTabla/referenciaId NO son FK:
// cada agente escribe sobre una tabla distinta (postulacion, evaluacion, etc).
@Entity
@Table(name = "trabajo_ia")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TrabajoIa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long organizacionId;
    private String agenteCodigo;
    private Long postulacionId;
    private String referenciaTabla;
    private Long referenciaId;
    private String estado;
    @Builder.Default
    private Integer intentos = 0;
    private Instant terminadoEn;
    private Instant creadoEn;
}
