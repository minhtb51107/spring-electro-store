package com.minh.springelectrostore.shared.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.minh.springelectrostore.shared.dto.response.FileUploadResponse;
import com.minh.springelectrostore.shared.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@Primary // Ưu tiên dùng Cloudinary
@RequiredArgsConstructor
@Slf4j
public class CloudinaryFileUploadServiceImpl implements FileUploadService {

    private final Cloudinary cloudinary;

    @Override
    public FileUploadResponse uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        String fileName = UUID.randomUUID().toString(); // Public ID trên Cloudinary

        try {
            // Upload lên Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", "electrostore/" + fileName,
                    "resource_type", "auto"
            ));

            String fileUrl = (String) uploadResult.get("secure_url");
            log.info("Upload file thành công lên Cloudinary: {}", fileUrl);

            return FileUploadResponse.builder()
                    .storedFilename(fileName) // Với Cloudinary, đây là public_id
                    .fileUrl(fileUrl)
                    .originalFilename(originalFilename)
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .message("Uploaded successfully to Cloudinary")
                    .build();

        } catch (IOException e) {
            log.error("Lỗi khi upload file lên Cloudinary", e);
            throw new IOException("Upload to cloud failed: " + e.getMessage());
        }
    }
}