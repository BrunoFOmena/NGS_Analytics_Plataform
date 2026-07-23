package com.ngs.analytics.upload;

import com.ngs.analytics.common.ApiException;
import com.ngs.analytics.common.NgsProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final NgsProperties properties;

    public StorageService(NgsProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() throws Exception {
        Path local = Path.of(properties.getStorage().getLocalDir()).toAbsolutePath().normalize();
        Files.createDirectories(local);
        log.info("Local storage directory: {}", local);
    }

    public StoredFile store(UUID sampleId, MultipartFile file) {
        if (file.getSize() > properties.getStorage().getMaxUploadBytes()) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds upload size limit");
        }
        String safeName = sanitize(file.getOriginalFilename());
        String relative = sampleId + "/" + UUID.randomUUID() + "_" + safeName;
        try {
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
