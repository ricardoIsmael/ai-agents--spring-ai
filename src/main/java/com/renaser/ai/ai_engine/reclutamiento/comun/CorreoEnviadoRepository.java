package com.renaser.ai.ai_engine.reclutamiento.comun;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorreoEnviadoRepository extends JpaRepository<CorreoEnviado, Long> {
    List<CorreoEnviado> findByUsuarioIdOrderByEnviadoEnDesc(Long usuarioId);
}
