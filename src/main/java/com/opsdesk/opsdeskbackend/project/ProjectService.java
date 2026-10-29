package com.opsdesk.opsdeskbackend.project;

import org.springframework.stereotype.Service;

import com.opsdesk.opsdeskbackend.common.NotFoundException;
import com.opsdesk.opsdeskbackend.common.PageResponse;

// Projects are read only in version 1 of the contract.
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public PageResponse<Project> findPage(int page, int size) {
        return PageResponse.fetch(page, size, projectRepository::findAll);
    }

    public Project findById(long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("project", String.valueOf(id)));
    }
}
