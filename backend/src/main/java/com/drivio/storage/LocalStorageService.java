package com.drivio.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "drivio.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${drivio.storage.upload-dir:./uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(MultipartFile file, String folder) throws IOException {
        Files.createDirectories(rootLocation);
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
        }
        String fileName = UUID.randomUUID() + extension;
        Path target = rootLocation.resolve(fileName).normalize();
        if (!target.startsWith(rootLocation)) {
            throw new IOException("Invalid storage path");
        }
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/" + fileName;
    }

    @Override
    public Resource loadAsResource(String storedPath) throws IOException {
        String fileName = storedPath.replace("/uploads/", "");
        Path filePath = rootLocation.resolve(fileName).normalize();
        if (!filePath.startsWith(rootLocation)) {
            throw new IOException("Invalid path traversal attempt");
        }
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("File not found: " + fileName);
        }
        return resource;
    }

    @Override
    public String getProviderName() {
        return "LOCAL";
    }
}
