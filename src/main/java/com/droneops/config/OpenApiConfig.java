package com.droneops.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI droneOpsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("SkyOps Drone Görev Planlama API")
                .version("1.0.0")
                .description("Operatörlerin drone filosu ile görev planlamasını yönettiği REST API."));
    }
}
