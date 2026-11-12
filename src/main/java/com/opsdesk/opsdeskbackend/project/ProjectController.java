package com.opsdesk.opsdeskbackend.project;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.opsdesk.opsdeskbackend.common.ErrorResponse;
import com.opsdesk.opsdeskbackend.common.PageResponse;
import com.opsdesk.opsdeskbackend.common.QueryParams;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Projects", description = "Read only in version 1.")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    @Operation(summary = "List projects")
    @ApiResponse(responseCode = "200", description = "One page of projects, oldest first.")
    @ApiResponse(responseCode = "400", description = "page or size is out of range.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PageResponse<ProjectResponse> list(
            @Parameter(description = "The page, counting from 0.",
                    schema = @Schema(type = "integer", minimum = "0", defaultValue = "0"))
            @RequestParam(required = false) String page,
            @Parameter(description = "How many projects on a page.",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20"))
            @RequestParam(required = false) String size) {
        QueryParams params = new QueryParams();
        int pageNumber = params.page(page);
        int pageSize = params.size(size);
        params.throwIfInvalid();
        return projectService.findPage(pageNumber, pageSize).map(ProjectResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one project")
    @ApiResponse(responseCode = "200", description = "The project.")
    @ApiResponse(responseCode = "404", description = "There is no project with this id.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ProjectResponse get(@PathVariable long id) {
        return ProjectResponse.from(projectService.findById(id));
    }
}
