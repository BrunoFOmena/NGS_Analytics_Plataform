package com.ngs.analytics.analytics;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "ngs.queue", name = "enabled", havingValue = "true")
public class AnalysisListener {

    private final AnalysisService analysisService;

    public AnalysisListener(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @RabbitListener(queues = "${ngs.queue.queue}")
    public void onMessage(AnalysisJobMessage message) {
        analysisService.process(message.analysisId());
    }
}
