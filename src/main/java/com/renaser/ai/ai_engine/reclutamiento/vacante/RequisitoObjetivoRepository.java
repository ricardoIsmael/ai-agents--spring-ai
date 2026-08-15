package com.renaser.ai.ai_engine.reclutamiento.vacante;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequisitoObjetivoRepository extends JpaRepository<RequisitoObjetivo, Long> {
    List<RequisitoObjetivo> findByVacanteIdAndEsActivoTrue(Long vacanteId);
    List<RequisitoObjetivo> findByVacanteId(Long vacanteId);
}
