package com.drivio.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface StorageService {
    String store(MultipartFile file, String folder) throws IOException;
    Resource loadAsResource(String storedPath) throws IOException;
    String getProviderName();
}
