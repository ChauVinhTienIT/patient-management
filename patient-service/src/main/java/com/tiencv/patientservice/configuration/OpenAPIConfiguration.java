package com.tiencv.patientservice.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Configuration
public class OpenAPIConfiguration {
    @Bean
    public OpenAPI customOpenAPI(
        @Value("${open.api.info.title}")       String title,
        @Value("${open.api.info.version}")     String version,
        @Value("${open.api.info.description}") String description,
        @Value("${open.api.info.server.url}")  String serverUrl,
        @Value("${open.api.info.server.description}") String serverDescription
    ) {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title(title)
                        .version(version)
                        .description(description)
                )
                .servers(List.of(new Server()
                        .url(serverUrl)
                        .description(serverDescription)
                ))
                .security(List.of(new SecurityRequirement()
                        .addList("bearerAuth")
                ));
    }

    @Bean
    public GroupedOpenApi groupedOpenApi (
        @Value("${open.api.info.group.name}") String groupName,
        @Value("${open.api.info.group.package-to-scan}") String packageToScan
    ) {
        return GroupedOpenApi.builder()
                .group(groupName)
                .packagesToScan(packageToScan)
                .build();
    }
}
