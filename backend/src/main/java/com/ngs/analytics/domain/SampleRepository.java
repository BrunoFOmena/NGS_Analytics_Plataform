package com.ngs.analytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SampleRepository extends JpaRepository<Sample, UUID> {
    List<Sample> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    Optional<Sample> findByIdAndProjectOwnerId(UUID id, UUID ownerId);
    List<Sample> findByProjectOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
