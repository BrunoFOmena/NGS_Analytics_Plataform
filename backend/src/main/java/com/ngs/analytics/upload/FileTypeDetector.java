package com.ngs.analytics.upload;

import com.ngs.analytics.domain.FileType;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class FileTypeDetector {

    public FileType detect(String filename) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (name.endsWith(".fastq.gz") || name.endsWith(".fq.gz")) {
            return FileType.FASTQ_GZ;
        }
        if (name.endsWith(".fastq") || name.endsWith(".fq")) {
            return FileType.FASTQ;
        }
        if (name.endsWith(".vcf.gz")) {
            return FileType.VCF_GZ;
        }
        if (name.endsWith(".vcf")) {
            return FileType.VCF;
        }
        if (name.endsWith(".fasta.gz") || name.endsWith(".fa.gz") || name.endsWith(".fna.gz")) {
            return FileType.FASTA_GZ;
        }
        if (name.endsWith(".fasta") || name.endsWith(".fa") || name.endsWith(".fna")) {
            return FileType.FASTA;
        }
        return FileType.UNKNOWN;
    }

    public boolean isGzipped(FileType type) {
        return type == FileType.FASTQ_GZ || type == FileType.VCF_GZ || type == FileType.FASTA_GZ;
    }

    public boolean isFastq(FileType type) {
        return type == FileType.FASTQ || type == FileType.FASTQ_GZ;
    }

    public boolean isVcf(FileType type) {
        return type == FileType.VCF || type == FileType.VCF_GZ;
    }

    public boolean isFasta(FileType type) {
        return type == FileType.FASTA || type == FileType.FASTA_GZ;
    }
}
