package com.ngs.analytics.fastq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngs.analytics.common.ApiException;
import com.ngs.analytics.domain.FastqMetrics;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;

@Component
public class FastqParser {

    private static final int OVERREP_SAMPLE_LIMIT = 50_000;
    private final ObjectMapper objectMapper;

    public FastqParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FastqMetrics parse(InputStream raw, boolean gzipped) {
        try (InputStream in = gzipped ? new GZIPInputStream(raw) : raw;
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return parseReader(reader);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to parse FASTQ: " + ex.getMessage());
        }
    }

    private FastqMetrics parseReader(BufferedReader reader) throws IOException {
        long readCount = 0;
        long totalLength = 0;
        int minLength = Integer.MAX_VALUE;
        int maxLength = 0;
        long basesA = 0, basesT = 0, basesC = 0, basesG = 0, basesN = 0, totalBases = 0;
        double qualitySum = 0;
        long qualityCount = 0;
        Map<Integer, Long> lengthDist = new TreeMap<>();
        Map<Integer, Double> posQualitySum = new HashMap<>();
        Map<Integer, Long> posQualityCount = new HashMap<>();
        Map<String, Long> sequenceCounts = new HashMap<>();
        Set<String> seen = new HashSet<>();
        long duplicates = 0;
        long sampled = 0;

        String header;
        while ((header = reader.readLine()) != null) {
            if (header.isBlank()) {
                continue;
            }
            if (!header.startsWith("@")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid FASTQ: expected '@' header");
            }
            String seq = reader.readLine();
            String plus = reader.readLine();
            String qual = reader.readLine();
            if (seq == null || plus == null || qual == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Truncated FASTQ record");
            }
            if (!plus.startsWith("+")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid FASTQ: expected '+' line");
            }

            int len = seq.length();
            readCount++;
            totalLength += len;
            minLength = Math.min(minLength, len);
            maxLength = Math.max(maxLength, len);
            lengthDist.merge(len, 1L, Long::sum);

            for (int i = 0; i < len; i++) {
                char b = Character.toUpperCase(seq.charAt(i));
                totalBases++;
                switch (b) {
                    case 'A' -> basesA++;
                    case 'T' -> basesT++;
                    case 'C' -> basesC++;
                    case 'G' -> basesG++;
                    default -> basesN++;
                }
            }

            int qLen = Math.min(len, qual.length());
            for (int i = 0; i < qLen; i++) {
                int phred = qual.charAt(i) - 33;
                qualitySum += phred;
                qualityCount++;
                int pos = i + 1;
                posQualitySum.merge(pos, (double) phred, Double::sum);
                posQualityCount.merge(pos, 1L, Long::sum);
            }

            if (sampled < OVERREP_SAMPLE_LIMIT) {
                sampled++;
                if (!seen.add(seq)) {
                    duplicates++;
                }
                sequenceCounts.merge(seq, 1L, Long::sum);
            }
        }

        if (readCount == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "FASTQ contains no reads");
        }

        FastqMetrics metrics = new FastqMetrics();
        metrics.setReadCount(readCount);
        metrics.setAvgLength(totalLength / (double) readCount);
        metrics.setMinLength(minLength == Integer.MAX_VALUE ? 0 : minLength);
        metrics.setMaxLength(maxLength);
        metrics.setGcContent(totalBases == 0 ? 0 : (basesG + basesC) * 100.0 / totalBases);
        metrics.setAtContent(totalBases == 0 ? 0 : (basesA + basesT) * 100.0 / totalBases);
        metrics.setNContent(totalBases == 0 ? 0 : basesN * 100.0 / totalBases);
        metrics.setMeanQuality(qualityCount == 0 ? 0 : qualitySum / qualityCount);
        metrics.setDuplicationRate(sampled == 0 ? 0 : duplicates * 100.0 / sampled);
        metrics.setLengthDistributionJson(toJson(lengthDist));
        metrics.setBaseCompositionJson(toJson(Map.of(
                "A", basesA, "T", basesT, "C", basesC, "G", basesG, "N", basesN
        )));

        Map<Integer, Double> perPos = new TreeMap<>();
        for (Integer pos : posQualitySum.keySet()) {
            perPos.put(pos, posQualitySum.get(pos) / posQualityCount.get(pos));
        }
        metrics.setPerPositionQualityJson(toJson(perPos));

        List<Map<String, Object>> overrep = sequenceCounts.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("sequence", e.getKey());
                    row.put("count", e.getValue());
                    return row;
                })
                .toList();
        metrics.setOverrepresentedJson(toJson(overrep));
        metrics.setPhredSummaryJson(toJson(Map.of(
                "mean", metrics.getMeanQuality(),
                "encoding", "Phred+33"
        )));
        return metrics;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "JSON serialization failed");
        }
    }
}
