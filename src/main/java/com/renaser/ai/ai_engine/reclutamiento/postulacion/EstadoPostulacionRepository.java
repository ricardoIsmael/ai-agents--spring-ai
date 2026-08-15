package com.renaser.ai.ai_engine.reclutamiento.postulacion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstadoPostulacionRepository extends JpaRepository<EstadoPostulacion, String> {
    List<EstadoPostulacion> findAllByOrderByOrden();
    Optional<EstadoPostulacion> findByEtapaCodigoAndMomentoCodigo(String etapaCodigo, String momentoCodigo);
}
