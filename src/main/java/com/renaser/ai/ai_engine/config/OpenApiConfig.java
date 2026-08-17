package com.renaser.ai.ai_engine.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI renaserOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("RENASER OS - AI Engine")
                        .description("Módulo de selección de personal (portal y panel) "
                                + "y orquestación de agentes IA")
                        .version("v2"))
                // El candado de Swagger: sin esto no hay dónde pegar el token y los
                // endpoints del portal y del panel no se pueden probar desde aquí
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("El token que devuelve /portal/auth/login (candidato) "
                                        + "o /panel/auth/dev-login (equipo)")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
