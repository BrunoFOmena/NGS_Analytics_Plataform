package com.ngs.analytics.vcf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngs.analytics.domain.VcfMetrics;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class VcfParserTest {

    private final VcfParser parser = new VcfParser(new ObjectMapper());

    @Test
    void parsesSyntheticVcf() {
        String vcf = """
                ##fileformat=VCFv4.2
                #CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO
                chr1\t100\t.\tA\tG\t99\tPASS\tDP=10;AF=0.5;GENE=BRCA1
                chr1\t200\t.\tAT\tA\t40\tLowQual\tDP=5;AF=0.1;GENE=TP53
                """;
        VcfMetrics metrics = parser.parse(new ByteArrayInputStream(vcf.getBytes(StandardCharsets.UTF_8)), false);
        assertEquals(2, metrics.getVariantCount());
        assertEquals(1, metrics.getSnpCount());
        assertEquals(1, metrics.getIndelCount());
        assertEquals(1, metrics.getPassCount());
        assertEquals(1, metrics.getFailCount());
        assertTrue(metrics.getTsTvRatio() >= 0);
    }
}
