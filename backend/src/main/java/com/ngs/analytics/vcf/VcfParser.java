package com.ngs.analytics.vcf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngs.analytics.common.ApiException;
import com.ngs.analytics.domain.VcfMetrics;
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
public class VcfParser {

    private static final Set<String> TRANSITIONS = Set.of("AG", "GA", "CT", "TC");

    private final ObjectMapper objectMapper;

    public VcfParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public VcfMetrics parse(InputStream raw, boolean gzipped) {
        try (InputStream in = gzipped ? new GZIPInputStream(raw) : raw;
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return parseReader(reader);
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Failed to parse VCF: " + ex.getMessage());
        }
    }

    private VcfMetrics parseReader(BufferedReader reader) throws IOException {
        long variantCount = 0, snp = 0, indel = 0, mnp = 0;
        double qualSum = 0;
        long qualCount = 0;
        double dpSum = 0;
        long dpCount = 0;
        long transitions = 0, transversions = 0;
        long pass = 0, fail = 0;
        Map<String, Long> chromDist = new TreeMap<>();
        Map<String, Long> filterDist = new TreeMap<>();
        Map<String, Long> geneDist = new TreeMap<>();
        List<Double> alleleFreqs = new ArrayList<>();

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] cols = line.split("\t");
            if (cols.length < 8) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid VCF row: expected at least 8 columns");
            }
            String chrom = cols[0];
            String ref = cols[3].toUpperCase(Locale.ROOT);
            String altField = cols[4].toUpperCase(Locale.ROOT);
            String qualStr = cols[5];
            String filter = cols[6];
            String info = cols[7];

            variantCount++;
            chromDist.merge(chrom, 1L, Long::sum);
            filterDist.merge(filter, 1L, Long::sum);
            if ("PASS".equalsIgnoreCase(filter) || ".".equals(filter)) {
                pass++;
            } else {
                fail++;
            }

            if (!".".equals(qualStr)) {
                try {
                    qualSum += Double.parseDouble(qualStr);
                    qualCount++;
                } catch (NumberFormatException ignored) {
                }
            }

            Integer dp = extractIntInfo(info, "DP");
            if (dp != null) {
                dpSum += dp;
                dpCount++;
            }
            Double af = extractFloatInfo(info, "AF");
            if (af != null) {
                alleleFreqs.add(af);
            }
            String gene = extractStringInfo(info, "GENE");
            if (gene != null) {
                geneDist.merge(gene, 1L, Long::sum);
            }

            String firstAlt = altField.split(",")[0];
            classify(ref, firstAlt, counts -> {
                // unused
            });
            if (ref.length() == 1 && firstAlt.length() == 1) {
                snp++;
                String pair = ref + firstAlt;
                if (TRANSITIONS.contains(pair)) {
                    transitions++;
                } else {
                    transversions++;
                }
            } else if (ref.length() != firstAlt.length()) {
                indel++;
            } else {
                mnp++;
            }
        }

        if (variantCount == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VCF contains no variants");
        }

        VcfMetrics metrics = new VcfMetrics();
        metrics.setVariantCount(variantCount);
        metrics.setSnpCount(snp);
        metrics.setIndelCount(indel);
        metrics.setMnpCount(mnp);
        metrics.setMeanQual(qualCount == 0 ? 0 : qualSum / qualCount);
        metrics.setMeanDp(dpCount == 0 ? 0 : dpSum / dpCount);
        metrics.setTransitions(transitions);
        metrics.setTransversions(transversions);
        metrics.setTsTvRatio(transversions == 0 ? transitions : transitions / (double) transversions);
        metrics.setPassCount(pass);
        metrics.setFailCount(fail);
        metrics.setChromosomeDistributionJson(toJson(chromDist));
        metrics.setFilterDistributionJson(toJson(filterDist));
        metrics.setGeneDistributionJson(toJson(geneDist));

        Map<String, Object> afSummary = new LinkedHashMap<>();
        if (!alleleFreqs.isEmpty()) {
            double sum = alleleFreqs.stream().mapToDouble(Double::doubleValue).sum();
            afSummary.put("count", alleleFreqs.size());
            afSummary.put("mean", sum / alleleFreqs.size());
            afSummary.put("min", alleleFreqs.stream().mapToDouble(Double::doubleValue).min().orElse(0));
            afSummary.put("max", alleleFreqs.stream().mapToDouble(Double::doubleValue).max().orElse(0));
            afSummary.put("values", alleleFreqs.stream().limit(100).toList());
        } else {
            afSummary.put("count", 0);
        }
        metrics.setAlleleFrequencyJson(toJson(afSummary));
        return metrics;
    }

    private void classify(String ref, String alt, java.util.function.Consumer<String> unused) {
        // kept for clarity / future extension
    }

    private Integer extractIntInfo(String info, String key) {
        String value = extractStringInfo(info, key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.split(",")[0]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double extractFloatInfo(String info, String key) {
        String value = extractStringInfo(info, key);
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.split(",")[0]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String extractStringInfo(String info, String key) {
        for (String part : info.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equalsIgnoreCase(key)) {
                return kv[1];
            }
        }
        return null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "JSON serialization failed");
        }
    }
}
