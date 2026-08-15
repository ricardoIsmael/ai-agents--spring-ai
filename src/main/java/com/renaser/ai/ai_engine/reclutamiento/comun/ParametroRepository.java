package com.renaser.ai.ai_engine.reclutamiento.comun;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParametroRepository extends JpaRepository<Parametro, Long> {
    Optional<Parametro> findByOrganizacionIdAndCodigo(Long organizacionId, String codigo);
    List<Parametro> findByOrganizacionIdOrderByCodigo(Long organizacionId);
}
