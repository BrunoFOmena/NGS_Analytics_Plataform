package com.ngs.spark;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight Spark local job used in Phase 2.
 * Memory tip: run with --driver-memory 1g --master local[2]
 */
public class NgsSparkApp {

    public static void main(String[] args) throws Exception {
        String mode = null;
        String input = null;
        String output = null;
        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--mode" -> mode = args[++i];
                case "--input" -> input = args[++i];
                case "--output" -> output = args[++i];
                default -> {
                }
            }
        }
        if (mode == null || input == null || output == null) {
            throw new IllegalArgumentException("Usage: --mode fastq|vcf --input <path> --output <json>");
        }

        SparkSession spark = SparkSession.builder()
                .appName("ngs-spark-jobs")
                .master(System.getProperty("spark.master", "local[2]"))
                .config("spark.driver.memory", System.getProperty("spark.driver.memory", "1g"))
                .config("spark.ui.enabled", "false")
                .getOrCreate();

        try (JavaSparkContext sc = new JavaSparkContext(spark.sparkContext())) {
            JavaRDD<String> lines = sc.textFile(input);
            long lineCount = lines.filter(l -> l != null && !l.isBlank()).count();
            Map<String, Object> result = new HashMap<>();
            result.put("mode", mode);
            result.put("input", input);
            result.put("lineCount", lineCount);
            result.put("engine", "spark-local");
            Path out = Path.of(output);
            Files.createDirectories(out.getParent() == null ? Path.of(".") : out.getParent());
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(out.toFile(), result);
        } finally {
            spark.stop();
        }
    }
}
