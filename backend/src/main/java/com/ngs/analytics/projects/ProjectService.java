package com.ngs.analytics.projects;

import com.ngs.analytics.common.ApiException;
import com.ngs.analytics.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SampleRepository sampleRepository;

    public ProjectService(ProjectRepository projectRepository, SampleRepository sampleRepository) {
        this.projectRepository = projectRepository;
        this.sampleRepository = sampleRepository;
    }

    @Transactional
    public Project create(UserAccount owner, ProjectDtos.CreateProjectRequest request) {
        Project project = new Project();
        project.setOwner(owner);
        project.setName(request.name().trim());
        project.setDescription(request.description());
        return projectRepository.save(project);
    }

    @Transactional(readOnly = true)
    public List<Project> list(UserAccount owner) {
        return projectRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId());
    }

    @Transactional(readOnly = true)
    public Project getOwned(UUID projectId, UserAccount owner) {
        return projectRepository.findByIdAndOwnerId(projectId, owner.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Project not found"));
    }

    @Transactional
    public Sample createSample(UUID projectId, UserAccount owner, ProjectDtos.CreateSampleRequest request) {
        Project project = getOwned(projectId, owner);
        Sample sample = new Sample();
        sample.setProject(project);
        sample.setName(request.name().trim());
        sample.setNotes(request.notes());
        sample.setFastaReferenceName(request.fastaReferenceName());
        return sampleRepository.save(sample);
    }

    @Transactional(readOnly = true)
    public List<Sample> listSamples(UUID projectId, UserAccount owner) {
        getOwned(projectId, owner);
        return sampleRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
    }

    @Transactional(readOnly = true)
    public Sample getSampleOwned(UUID sampleId, UserAccount owner) {
        return sampleRepository.findByIdAndProjectOwnerId(sampleId, owner.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Sample not found"));
    }

    @Transactional
    public Sample updateFastaMetadata(UUID sampleId, UserAccount owner, String referenceName, String storagePath) {
        Sample sample = getSampleOwned(sampleId, owner);
        sample.setFastaReferenceName(referenceName);
        sample.setFastaStoragePath(storagePath);
        return sampleRepository.save(sample);
    }
}
