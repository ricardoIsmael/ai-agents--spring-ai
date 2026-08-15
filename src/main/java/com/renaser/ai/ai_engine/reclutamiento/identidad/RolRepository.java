package com.renaser.ai.ai_engine.reclutamiento.identidad;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {
    Optional<Rol> findByOrganizacionIdAndCodigo(Long organizacionId, String codigo);
    List<Rol> findByOrganizacionIdOrderByCodigo(Long organizacionId);
}
