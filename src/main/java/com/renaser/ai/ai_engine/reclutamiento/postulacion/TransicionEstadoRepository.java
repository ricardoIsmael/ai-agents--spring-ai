package com.renaser.ai.ai_engine.reclutamiento.postulacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransicionEstadoRepository extends JpaRepository<TransicionEstado, Long> {
    List<TransicionEstado> findByPostulacionIdOrderByOcurridaEnAsc(Long postulacionId);
}
