package com.renaser.ai.ai_engine.reclutamiento.comun;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    Page<Auditoria> findByOrganizacionIdOrderByOcurridaEnDesc(Long organizacionId, Pageable pageable);
    Page<Auditoria> findByOrganizacionIdAndEntidadOrderByOcurridaEnDesc(
            Long organizacionId, String entidad, Pageable pageable);
}
