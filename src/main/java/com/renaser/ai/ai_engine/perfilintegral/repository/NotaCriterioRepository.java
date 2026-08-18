package com.renaser.ai.ai_engine.perfilintegral.repository;

import com.renaser.ai.ai_engine.perfilintegral.entity.NotaCriterio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotaCriterioRepository extends JpaRepository<NotaCriterio, Long> {

    List<NotaCriterio> findByPostulacionId(Long postulacionId);
    Optional<NotaCriterio> findByPostulacionIdAndCriterioId(Long postulacionId, Long criterioId);
}
