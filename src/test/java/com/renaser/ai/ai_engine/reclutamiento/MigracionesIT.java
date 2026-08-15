package com.renaser.ai.ai_engine.reclutamiento;

import com.renaser.ai.ai_engine.reclutamiento.postulacion.EstadoPostulacion;
import com.renaser.ai.ai_engine.reclutamiento.postulacion.EstadoPostulacionRepository;
import com.renaser.ai.ai_engine.reclutamiento.identidad.OrganizacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Prueba las migraciones donde van a correr: un Postgres real y efímero. Flyway migra
// desde cero y Hibernate valida que cada entidad coincida con lo que las migraciones
// crearon. Si un tipo o una columna no cuadra, este test no arranca.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
public class MigracionesIT {

    @Container
    @ServiceConnection
    // La misma imagen que producción: pgvector/pg16, no un Postgres pelado
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @Autowired
    private EstadoPostulacionRepository estados;

    @Autowired
    private OrganizacionRepository organizaciones;

    @Test
    void lasSemillasDejanElCatalogoCompleto() {
        List<EstadoPostulacion> todos = estados.findAllByOrderByOrden();
        assertThat(todos).hasSize(18);
        assertThat(todos.stream().filter(EstadoPostulacion::isEsFinal)).hasSize(3);
        // La rejilla: cada estado no final ni de entrada tiene etapa y momento
        assertThat(todos.stream()
                .filter(e -> !e.isEsFinal() && !"POSTULADA".equals(e.getCodigo()))
                .allMatch(e -> e.getEtapaCodigo() != null && e.getMomentoCodigo() != null))
                .isTrue();
        assertThat(organizaciones.findByCodigo("RENASER")).isPresent();
    }
}
