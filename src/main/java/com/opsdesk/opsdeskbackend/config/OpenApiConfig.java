package com.opsdesk.opsdeskbackend.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.opsdesk.opsdeskbackend.common.ErrorResponse;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;

// The title of the generated API documentation (/v3/api-docs and the Swagger
// page at /swagger-ui.html), and how to log in on it.
//
// @SecurityScheme describes the login: a JWT sent as
// "Authorization: Bearer <token>". It adds an Authorize button to the Swagger
// page, where the token from register or login is pasted once and then sent
// with every request tried there. @SecurityRequirement makes every endpoint
// need it, except register and login, which turn it off for themselves.
//
// API_CONTRACT.md in opsdesk-frontend is the source of truth. These docs are
// generated from the code and must say the same thing.
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "OpsDesk API",
                version = "1",
                description = "The REST API for OpsDesk, an IT support ticketing app. "
                        + "The full rules and error messages are in API_CONTRACT.md in opsdesk-frontend. "
                        + "To try an endpoint here, use POST /api/v1/auth/login (or register), "
                        + "copy the token from the answer, click Authorize and paste it without \"Bearer\"."),
        security = @SecurityRequirement(name = OpenApiConfig.BEARER))
@SecurityScheme(
        name = OpenApiConfig.BEARER,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "The token from POST /api/v1/auth/register or /api/v1/auth/login. It lasts 24 hours.")
public class OpenApiConfig {

    public static final String BEARER = "bearerAuth";

    private static final String ERROR_SCHEMA = "ErrorResponse";

    // Two answers any endpoint can give, added here once instead of on every
    // method: 401 on every endpoint that needs a token, and 500 everywhere.
    // Answers only some endpoints give (400, 403, 404, 409) are on the
    // controller methods.
    @Bean
    public OpenApiCustomizer commonErrorResponses() {
        return openApi -> {
            Components components = openApi.getComponents();
            if (components.getSchemas() == null || !components.getSchemas().containsKey(ERROR_SCHEMA)) {
                ModelConverters.getInstance().read(ErrorResponse.class).forEach(components::addSchemas);
            }
            openApi.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                ApiResponses responses = operation.getResponses();
                boolean needsToken = operation.getSecurity() == null || !operation.getSecurity().isEmpty();
                if (needsToken) {
                    responses.putIfAbsent("401", error("No token, or an expired or invalid one: "
                            + "{\"error\": \"UNAUTHENTICATED\", \"message\": \"Log in to continue.\"}"));
                }
                responses.putIfAbsent("500", error("Something went wrong on the server: "
                        + "{\"error\": \"INTERNAL_ERROR\", \"message\": \"Something went wrong on the server.\"}"));
            }));
        };
    }

    private static ApiResponse error(String description) {
        Schema<?> schema = new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA);
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", new MediaType().schema(schema)));
    }
}
