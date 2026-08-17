package com.renaser.ai.ai_engine.perfilintegral.repository;

import com.renaser.ai.ai_engine.perfilintegral.entity.Opcion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpcionRepository extends JpaRepository<Opcion, Long> {
    List<Opcion> findByPreguntaIdOrderByLetra(Long preguntaId);
}
