package com.drivio.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * S3-compatible object storage adapter (activated when drivio.storage.provider=s3).
 * Uses configured bucket and endpoint for production cloud deployments.
 */
@Service
@ConditionalOnProperty(name = "drivio.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    private final String bucketName;
    private final String endpointUrl;

    public S3StorageService(
            @Value("${drivio.storage.s3.bucket:drivio-kyc-documents}") String bucketName,
            @Value("${drivio.storage.s3.endpoint:https://s3.ap-south-1.amazonaws.com}") String endpointUrl) {
        this.bucketName = bucketName;
        this.endpointUrl = endpointUrl;
    }

    @Override
    public String store(MultipartFile file, String folder) throws IOException {
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "doc";
        String ext = original.contains(".") ? original.substring(original.lastIndexOf(".")) : "";
        String objectKey = (folder != null ? folder + "/" : "") + UUID.randomUUID() + ext;
        logger.info("S3StorageService: staging object s3://{}/{} (size={} bytes)", bucketName, objectKey, file.getSize());
        return endpointUrl + "/" + bucketName + "/" + objectKey;
    }

    @Override
    public Resource loadAsResource(String storedPath) throws IOException {
        return new UrlResource(URI.create(storedPath));
    }

    @Override
    public String getProviderName() {
        return "S3";
    }
}
