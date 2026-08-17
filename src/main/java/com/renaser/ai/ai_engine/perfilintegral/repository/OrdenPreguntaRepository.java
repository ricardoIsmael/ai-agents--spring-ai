package com.renaser.ai.ai_engine.perfilintegral.repository;

import com.renaser.ai.ai_engine.perfilintegral.entity.OrdenPregunta;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdenPreguntaRepository extends JpaRepository<OrdenPregunta, OrdenPregunta.Clave> {
}
