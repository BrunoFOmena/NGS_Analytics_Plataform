package com.ngs.analytics.fastq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ngs.analytics.domain.FastqMetrics;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FastqParserTest {

    private final FastqParser parser = new FastqParser(new ObjectMapper());

    @Test
    void parsesSyntheticFastq() {
        String fastq = """
                @r1
                ACGTNN
                +
                IIIIII
                @r2
                AAAA
                +
                !!!!
                """;
        FastqMetrics metrics = parser.parse(new ByteArrayInputStream(fastq.getBytes(StandardCharsets.UTF_8)), false);
        assertEquals(2, metrics.getReadCount());
        assertEquals(4, metrics.getMinLength());
        assertEquals(6, metrics.getMaxLength());
        assertTrue(metrics.getNContent() > 0);
        assertNotNull(metrics.getBaseCompositionJson());
    }
}
