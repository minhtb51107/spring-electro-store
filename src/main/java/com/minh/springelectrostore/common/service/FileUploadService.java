package com.minh.springelectrostore.common.service;

import org.springframework.web.multipart.MultipartFile;

import com.minh.springelectrostore.common.dto.response.FileUploadResponse;

import java.io.IOException;

public interface FileUploadService {
    /**
     * Upload file (lên Server hoặc Cloud) và trả về thông tin chi tiết.
     * @param file Đối tượng MultipartFile từ request.
     * @return DTO chứa thông tin file (URL, tên file, kích thước...).
     * @throws IOException Nếu có lỗi khi lưu file.
     */
    FileUploadResponse uploadFile(MultipartFile file) throws IOException;
}