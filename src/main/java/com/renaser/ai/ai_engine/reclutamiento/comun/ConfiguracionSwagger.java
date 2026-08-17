package com.renaser.ai.ai_engine.reclutamiento.comun;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * El candado de Swagger para el portal y el panel.
 *
 * <p>No toca el bean {@code OpenAPI} del módulo de agentes: lo completa desde fuera. Así el
 * botón «Authorize» aparece sin que haya que editar un archivo compartido, y el candado se
 * pone solo en nuestros endpoints, no en los de agentes, que hoy van abiertos.
 */
@Configuration
public class ConfiguracionSwagger {

    private static final String ESQUEMA = "bearer";
    private static final String PAQUETE_RECLUTAMIENTO = "com.renaser.ai.ai_engine.reclutamiento";

    // Sin esto no hay dónde pegar el token y los endpoints no se pueden probar desde Swagger
    @Bean
    public OpenApiCustomizer esquemaBearerReclutamiento() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            openApi.getComponents().addSecuritySchemes(ESQUEMA, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("El token que devuelve /portal/auth/login (candidato) "
                            + "o /panel/auth/dev-login (equipo)"));
        };
    }

    // El candado solo en nuestras operaciones: las de agentes no piden token todavía
    @Bean
    public OperationCustomizer candadoSoloEnReclutamiento() {
        return (operacion, metodo) -> {
            String paquete = metodo.getBeanType().getPackageName();
            if (paquete.startsWith(PAQUETE_RECLUTAMIENTO)) {
                operacion.addSecurityItem(new SecurityRequirement().addList(ESQUEMA));
            }
            return operacion;
        };
    }
}
