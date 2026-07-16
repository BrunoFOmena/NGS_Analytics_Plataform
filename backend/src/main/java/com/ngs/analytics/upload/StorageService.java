package com.ngs.analytics.upload;

import com.ngs.analytics.common.ApiException;
import com.ngs.analytics.common.NgsProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final NgsProperties properties;
    private MinioClient minioClient;

    public StorageService(NgsProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() throws Exception {
        Path local = Path.of(properties.getStorage().getLocalDir()).toAbsolutePath().normalize();
        Files.createDirectories(local);
        if (properties.getMinio().isEnabled()) {
            minioClient = MinioClient.builder()
                    .endpoint(properties.getMinio().getEndpoint())
                    .credentials(properties.getMinio().getAccessKey(), properties.getMinio().getSecretKey())
                    .build();
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.getMinio().getBucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.getMinio().getBucket()).build());
            }
            log.info("MinIO storage enabled at {}", properties.getMinio().getEndpoint());
        } else {
            log.info("Local storage directory: {}", local);
        }
    }

    public StoredFile store(UUID sampleId, MultipartFile file) {
        if (file.getSize() > properties.getStorage().getMaxUploadBytes()) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds upload size limit");
        }
        String safeName = sanitize(file.getOriginalFilename());
        String relative = sampleId + "/" + UUID.randomUUID() + "_" + safeName;
        try {
            if (properties.getMinio().isEnabled() && "minio".equalsIgnoreCase(properties.getStorage().getType())) {
                try (InputStream in = file.getInputStream()) {
                    minioClient.putObject(PutObjectArgs.builder()
                            .bucket(properties.getMinio().getBucket())
                            .object(relative)
                            .stream(in, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
                }
                return new StoredFile("minio://" + properties.getMinio().getBucket() + "/" + relative, file.getSize());
            }
            Path target = Path.of(properties.getStorage().getLocalDir()).toAbsolutePath().normalize().resolve(relative);
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(target.toString(), Files.size(target));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file: " + ex.getMessage());
        }
    }

    public InputStream open(String storagePath) {
        try {
            if (storagePath.startsWith("minio://")) {
                if (minioClient == null) {
                    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "MinIO is not enabled");
                }
                String without = storagePath.substring("minio://".length());
                int slash = without.indexOf('/');
                String bucket = without.substring(0, slash);
                String object = without.substring(slash + 1);
                return minioClient.getObject(io.minio.GetObjectArgs.builder().bucket(bucket).object(object).build());
            }
            return Files.newInputStream(Path.of(storagePath));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Stored file not readable: " + ex.getMessage());
        }
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "upload.bin";
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public record StoredFile(String path, long sizeBytes) {
    }
}
