package com.renaser.ai.ai_engine.perfilintegral.repository;

import com.renaser.ai.ai_engine.perfilintegral.entity.HallazgoPerfil;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HallazgoPerfilRepository extends JpaRepository<HallazgoPerfil, Long> {

    java.util.List<HallazgoPerfil> findByPerfilTalentoId(Long perfilTalentoId);

    void deleteByPerfilTalentoId(Long perfilTalentoId);
}
