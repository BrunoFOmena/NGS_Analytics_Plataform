package com.ngs.analytics.analytics;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AnalysisRunner {

    private final AnalysisService analysisService;

    public AnalysisRunner(@Lazy AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @Async("taskExecutor")
    public void runAsync(UUID analysisId) {
        analysisService.process(analysisId);
    }
}
