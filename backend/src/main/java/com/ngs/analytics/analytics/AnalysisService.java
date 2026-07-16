package com.ngs.analytics.analytics;

import com.ngs.analytics.common.ApiException;
import com.ngs.analytics.common.NgsProperties;
import com.ngs.analytics.domain.*;
import com.ngs.analytics.fastq.FastqParser;
import com.ngs.analytics.upload.FileTypeDetector;
import com.ngs.analytics.upload.StorageService;
import com.ngs.analytics.vcf.VcfParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final AnalysisRepository analysisRepository;
    private final FastqMetricsRepository fastqMetricsRepository;
    private final VcfMetricsRepository vcfMetricsRepository;
    private final StorageService storageService;
    private final FastqParser fastqParser;
    private final VcfParser vcfParser;
    private final FileTypeDetector fileTypeDetector;
    private final NgsProperties properties;
    private final ObjectProvider<RabbitTemplate> rabbitTemplate;
    private final AnalysisRunner analysisRunner;

    public AnalysisService(
            AnalysisRepository analysisRepository,
            FastqMetricsRepository fastqMetricsRepository,
            VcfMetricsRepository vcfMetricsRepository,
            StorageService storageService,
            FastqParser fastqParser,
            VcfParser vcfParser,
            FileTypeDetector fileTypeDetector,
            NgsProperties properties,
            ObjectProvider<RabbitTemplate> rabbitTemplate,
            @org.springframework.context.annotation.Lazy AnalysisRunner analysisRunner
    ) {
        this.analysisRepository = analysisRepository;
        this.fastqMetricsRepository = fastqMetricsRepository;
        this.vcfMetricsRepository = vcfMetricsRepository;
        this.storageService = storageService;
        this.fastqParser = fastqParser;
        this.vcfParser = vcfParser;
        this.fileTypeDetector = fileTypeDetector;
        this.properties = properties;
        this.rabbitTemplate = rabbitTemplate;
        this.analysisRunner = analysisRunner;
    }

    @Transactional
    public Analysis enqueue(Sample sample, FileAsset fileAsset) {
        Analysis analysis = new Analysis();
        analysis.setSample(sample);
        analysis.setFileAsset(fileAsset);
        analysis.setStatus(AnalysisStatus.QUEUED);
        analysis.setEngine("JAVA");
        analysisRepository.save(analysis);

        UUID analysisId = analysis.getId();
        Runnable dispatch = () -> {
            if (properties.getQueue().isEnabled()) {
                RabbitTemplate template = rabbitTemplate.getIfAvailable();
                if (template != null) {
                    template.convertAndSend(
                            properties.getQueue().getExchange(),
                            properties.getQueue().getRoutingKey(),
                            new AnalysisJobMessage(analysisId)
                    );
                    return;
                }
            }
            analysisRunner.runAsync(analysisId);
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
        return analysis;
    }

    @Transactional
    public void process(UUID analysisId) {
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Analysis not found"));
        analysis.setStatus(AnalysisStatus.RUNNING);
        analysis.setStartedAt(Instant.now());
        analysisRepository.save(analysis);

        try {
            FileAsset file = analysis.getFileAsset();
            FileType type = file.getFileType();
            boolean gzipped = fileTypeDetector.isGzipped(type);

            if (fileTypeDetector.isFastq(type)) {
                try (InputStream in = storageService.open(file.getStoragePath())) {
                    FastqMetrics metrics = fastqParser.parse(in, gzipped);
                    metrics.setAnalysis(analysis);
                    fastqMetricsRepository.save(metrics);
                }
                analysis.setEngine("JAVA");
            } else if (fileTypeDetector.isVcf(type)) {
                try (InputStream in = storageService.open(file.getStoragePath())) {
                    VcfMetrics metrics = vcfParser.parse(in, gzipped);
                    metrics.setAnalysis(analysis);
                    vcfMetricsRepository.save(metrics);
                }
                analysis.setEngine("JAVA");
            } else if (fileTypeDetector.isFasta(type)) {
                // FASTA is stored as reference metadata only in Phase 3
                analysis.setEngine("METADATA");
            } else {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported file type for analysis");
            }

            analysis.setStatus(AnalysisStatus.DONE);
            analysis.setFinishedAt(Instant.now());
            analysis.setErrorMessage(null);
            analysisRepository.save(analysis);
        } catch (Exception ex) {
            log.error("Analysis {} failed", analysisId, ex);
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setFinishedAt(Instant.now());
            analysis.setErrorMessage(ex.getMessage());
            analysisRepository.save(analysis);
        }
    }

    @Transactional(readOnly = true)
    public Analysis get(UUID id, UUID ownerId) {
        Analysis analysis = analysisRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Analysis not found"));
        if (!analysis.getSample().getProject().getOwner().getId().equals(ownerId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not allowed");
        }
        return analysis;
    }

    @Transactional(readOnly = true)
    public List<Analysis> historyForOwner(UUID ownerId) {
        return analysisRepository.findBySampleProjectOwnerIdOrderByCreatedAtDesc(ownerId);
    }
}
