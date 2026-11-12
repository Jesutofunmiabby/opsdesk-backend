package com.opsdesk.opsdeskbackend.project;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;

@Schema(name = "Project")
public record ProjectResponse(
        @Schema(requiredMode = RequiredMode.REQUIRED, example = "1")
        long id,

        @Schema(requiredMode = RequiredMode.REQUIRED, minLength = 1, maxLength = 120,
                example = "Laptop refresh for the sales team")
        String name,

        @Schema(requiredMode = RequiredMode.REQUIRED, description = "The person responsible, as a display name.",
                example = "Amara Okafor")
        String owner,

        @Schema(requiredMode = RequiredMode.REQUIRED)
        ProjectStatus status,

        @Schema(requiredMode = RequiredMode.REQUIRED, format = "date", description = "A calendar day: YYYY-MM-DD.",
                example = "2026-10-23")
        String dueDate) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getOwner(),
                project.getStatus(),
                // A calendar day as the contract writes it: 2026-10-23.
                project.getDueDate().toString());
    }
}
