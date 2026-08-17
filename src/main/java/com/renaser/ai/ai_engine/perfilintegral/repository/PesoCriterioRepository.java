package com.renaser.ai.ai_engine.perfilintegral.repository;

import com.renaser.ai.ai_engine.perfilintegral.entity.PesoCriterio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PesoCriterioRepository extends JpaRepository<PesoCriterio, PesoCriterio.Clave> {
    List<PesoCriterio> findByVersionPesosId(Long versionPesosId);
}
