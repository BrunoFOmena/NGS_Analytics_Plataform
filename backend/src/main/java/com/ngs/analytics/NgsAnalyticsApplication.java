package com.ngs.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ConfigurationPropertiesScan
public class NgsAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(NgsAnalyticsApplication.class, args);
    }
}
