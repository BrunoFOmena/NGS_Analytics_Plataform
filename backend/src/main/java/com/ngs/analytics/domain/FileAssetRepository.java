package com.ngs.analytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FileAssetRepository extends JpaRepository<FileAsset, UUID> {
    List<FileAsset> findBySampleIdOrderByUploadedAtDesc(UUID sampleId);
}
