package com.renaser.ai.ai_engine.perfilintegral.repository;

import com.renaser.ai.ai_engine.perfilintegral.entity.Criterio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CriterioRepository extends JpaRepository<Criterio, Long> {

    // La rúbrica de una versión de prueba concreta: los del CV (hito 2) tienen este
    // campo vacío, así que no se mezclan.
    List<Criterio> findByVersionPlantillaPruebaId(Long versionPlantillaPruebaId);
}
