package com.ngs.analytics.projects;

import com.ngs.analytics.auth.SecurityUtils;
import com.ngs.analytics.domain.Project;
import com.ngs.analytics.domain.Sample;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ProjectDtos.ProjectResponse create(@Valid @RequestBody ProjectDtos.CreateProjectRequest request) {
        return toProject(projectService.create(SecurityUtils.currentUser(), request));
    }

    @GetMapping
    public List<ProjectDtos.ProjectResponse> list() {
        return projectService.list(SecurityUtils.currentUser()).stream().map(this::toProject).toList();
    }

    @GetMapping("/{projectId}")
    public ProjectDtos.ProjectResponse get(@PathVariable UUID projectId) {
        return toProject(projectService.getOwned(projectId, SecurityUtils.currentUser()));
    }

    @PostMapping("/{projectId}/samples")
    public ProjectDtos.SampleResponse createSample(
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectDtos.CreateSampleRequest request
    ) {
        return toSample(projectService.createSample(projectId, SecurityUtils.currentUser(), request));
    }

    @GetMapping("/{projectId}/samples")
    public List<ProjectDtos.SampleResponse> listSamples(@PathVariable UUID projectId) {
        return projectService.listSamples(projectId, SecurityUtils.currentUser()).stream().map(this::toSample).toList();
    }

    private ProjectDtos.ProjectResponse toProject(Project p) {
        return new ProjectDtos.ProjectResponse(p.getId().toString(), p.getName(), p.getDescription(), p.getCreatedAt());
    }

    private ProjectDtos.SampleResponse toSample(Sample s) {
        return new ProjectDtos.SampleResponse(
                s.getId().toString(),
                s.getProject().getId().toString(),
                s.getName(),
                s.getNotes(),
                s.getFastaReferenceName(),
                s.getFastaStoragePath(),
                s.getCreatedAt()
        );
    }
}
