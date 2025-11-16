package com.minh.springelectrostore.shared.service.impl;

import com.minh.springelectrostore.shared.dto.response.FileUploadResponse;
import com.minh.springelectrostore.shared.service.FileUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
// Class này không có @Primary, nên Spring sẽ ưu tiên CloudinaryService
public class LocalFileUploadServiceImpl implements FileUploadService {

    private final Path rootLocation = Paths.get("uploads");

    public LocalFileUploadServiceImpl() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage!", e);
        }
    }

    @Override
    public FileUploadResponse uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Failed to store empty file.");
        }

        // 1. Tạo tên file duy nhất
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFilename = UUID.randomUUID().toString() + extension;

        // 2. Lưu file vào ổ cứng
        Path destinationFile = this.rootLocation.resolve(Paths.get(storedFilename))
                .normalize().toAbsolutePath();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // 3. Tạo URL truy cập (Local URL)
        // Ví dụ: http://localhost:8080/uploads/abc-xyz.jpg
        String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(storedFilename)
                .toUriString();

        // 4. Trả về DTO chuẩn
        return FileUploadResponse.builder()
                .storedFilename(storedFilename)
                .fileUrl(fileUrl)
                .originalFilename(originalFilename)
                .contentType(file.getContentType())
                .size(file.getSize())
                .message("Uploaded successfully to Local Storage")
                .build();
    }
}