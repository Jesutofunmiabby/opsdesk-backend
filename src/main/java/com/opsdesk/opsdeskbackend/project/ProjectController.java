package com.opsdesk.opsdeskbackend.project;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.opsdesk.opsdeskbackend.common.PageResponse;
import com.opsdesk.opsdeskbackend.common.QueryParams;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public PageResponse<ProjectResponse> list(
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size) {
        QueryParams params = new QueryParams();
        int pageNumber = params.page(page);
        int pageSize = params.size(size);
        params.throwIfInvalid();
        return projectService.findPage(pageNumber, pageSize).map(ProjectResponse::from);
    }

    @GetMapping("/{id}")
    public ProjectResponse get(@PathVariable long id) {
        return ProjectResponse.from(projectService.findById(id));
    }
}
