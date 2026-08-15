package com.renaser.ai.ai_engine.reclutamiento.postulacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CvRepository extends JpaRepository<Cv, Long> {
    Optional<Cv> findByPostulacionId(Long postulacionId);
}
