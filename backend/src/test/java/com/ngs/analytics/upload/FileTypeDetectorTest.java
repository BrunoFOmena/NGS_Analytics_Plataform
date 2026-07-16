package com.ngs.analytics.upload;

import com.ngs.analytics.domain.FileType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileTypeDetectorTest {

    private final FileTypeDetector detector = new FileTypeDetector();

    @Test
    void detectsCompressedAndPlainFormats() {
        assertEquals(FileType.FASTQ_GZ, detector.detect("reads.fastq.gz"));
        assertEquals(FileType.VCF, detector.detect("variants.vcf"));
        assertEquals(FileType.FASTA, detector.detect("ref.fa"));
        assertTrue(detector.isGzipped(FileType.VCF_GZ));
        assertTrue(detector.isFastq(FileType.FASTQ));
    }
}
