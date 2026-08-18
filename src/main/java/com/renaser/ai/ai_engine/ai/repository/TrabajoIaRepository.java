package com.renaser.ai.ai_engine.ai.repository;

import com.renaser.ai.ai_engine.ai.model.TrabajoIa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TrabajoIaRepository extends JpaRepository<TrabajoIa, Long> {

    List<TrabajoIa> findByPostulacionIdOrderByIdAsc(Long postulacionId);

    Optional<TrabajoIa> findFirstByPostulacionIdAndAgenteCodigoOrderByIdDesc(
            Long postulacionId, String agenteCodigo);

    // Los que el sondeo tiene que volver a empujar: o nadie recogió el mensaje, o quien lo
    // recogió se murió a mitad. Ver ReintentoTrabajosIa.
    List<TrabajoIa> findByEstadoAndCreadoEnBefore(String estado, Instant limite);

    List<TrabajoIa> findByEstadoAndTomadoEnBefore(String estado, Instant limite);
}
