package com.ngs.analytics.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FastqMetricsRepository extends JpaRepository<FastqMetrics, UUID> {
    Optional<FastqMetrics> findByAnalysisId(UUID analysisId);
    Optional<FastqMetrics> findTopByAnalysisSampleIdAndAnalysisStatusOrderByAnalysisFinishedAtDesc(
            UUID sampleId, AnalysisStatus status);
}
