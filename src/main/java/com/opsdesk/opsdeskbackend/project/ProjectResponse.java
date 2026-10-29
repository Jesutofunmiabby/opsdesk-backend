package com.opsdesk.opsdeskbackend.project;

public record ProjectResponse(
        long id,
        String name,
        String owner,
        ProjectStatus status,
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
