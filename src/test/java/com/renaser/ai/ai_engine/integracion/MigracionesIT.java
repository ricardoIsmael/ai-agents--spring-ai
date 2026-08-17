package com.renaser.ai.ai_engine.integracion;

import com.renaser.ai.ai_engine.postulacion.entity.EstadoPostulacion;
import com.renaser.ai.ai_engine.postulacion.repository.EstadoPostulacionRepository;
import com.renaser.ai.ai_engine.organizacion.repository.OrganizacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("app.seguridad.jwt-secreto",
                () -> "clave-de-pruebas-suficientemente-larga-para-hmac-256-bits");
        registro.add("spring.ai.deepseek.api-key", () -> "clave-de-pruebas-no-se-usa");
    }

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
