package com.ngs.analytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VcfMetricsRepository extends JpaRepository<VcfMetrics, UUID> {
    Optional<VcfMetrics> findByAnalysisId(UUID analysisId);
    Optional<VcfMetrics> findTopByAnalysisSampleIdAndAnalysisStatusOrderByAnalysisFinishedAtDesc(
            UUID sampleId, AnalysisStatus status);
}
