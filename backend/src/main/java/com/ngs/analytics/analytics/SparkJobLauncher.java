package com.ngs.analytics.analytics;

import com.ngs.analytics.common.NgsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Optional launcher that shells out to spark-submit when the spark-jobs JAR is present.
 * Falls back silently so Phase 1 Java parsers remain the default path.
 */
@Component
public class SparkJobLauncher {

    private static final Logger log = LoggerFactory.getLogger(SparkJobLauncher.class);

    private final NgsProperties properties;

    public SparkJobLauncher(NgsProperties properties) {
        this.properties = properties;
    }

    public boolean tryRun(String mode, String inputPath, String outputJsonPath) {
        if (!properties.getSpark().isEnabled()) {
            return false;
        }
        Path jar = Path.of(properties.getSpark().getJarPath()).toAbsolutePath().normalize();
        if (!Files.exists(jar)) {
            log.warn("Spark jar not found at {}, using Java parser", jar);
            return false;
        }
        List<String> cmd = new ArrayList<>();
        cmd.add("spark-submit");
        cmd.add("--master");
        cmd.add(properties.getSpark().getMaster());
        cmd.add("--driver-memory");
        cmd.add(properties.getSpark().getDriverMemory());
        cmd.add("--class");
        cmd.add("com.ngs.spark.NgsSparkApp");
        cmd.add(jar.toString());
        cmd.add("--mode");
        cmd.add(mode);
        cmd.add("--input");
        cmd.add(inputPath);
        cmd.add("--output");
        cmd.add(outputJsonPath);

        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Spark job timed out");
                return false;
            }
            if (process.exitValue() != 0) {
                log.warn("Spark job failed with exit {}", process.exitValue());
                return false;
            }
            return Files.exists(Path.of(outputJsonPath));
        } catch (Exception ex) {
            log.warn("Spark launch failed: {}", ex.getMessage());
            return false;
        }
    }
}
