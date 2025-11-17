package com.minh.springelectrostore.product.service.impl;

import com.minh.springelectrostore.product.dto.request.BrandRequest;
import com.minh.springelectrostore.product.dto.response.BrandResponse;
import com.minh.springelectrostore.product.entity.Brand;
import com.minh.springelectrostore.product.mapper.BrandMapper;
import com.minh.springelectrostore.product.repository.BrandRepository;
// TODO: (Module 2) Bỏ comment dòng này khi ProductRepository tồn tại
// import com.minh.springelectrostore.product.repository.ProductRepository;
import com.minh.springelectrostore.product.service.BrandService;
import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import com.minh.springelectrostore.shared.util.SlugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Tự động inject các dependency (final)
@Slf4j // Dùng để log
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;
    private final SlugService slugService;
    // TODO: (Module 2) Bỏ comment dòng này khi ProductRepository tồn tại
    // private final ProductRepository productRepository;

    @Override
    @Transactional // Đảm bảo toàn bộ hàm này là một giao dịch CSDL (atomic)
    public BrandResponse createBrand(BrandRequest request) {
        // 1. Kiểm tra logic nghiệp vụ: Tên thương hiệu không được trùng
        if (brandRepository.findByName(request.getName()).isPresent()) {
            throw new BadRequestException("Tên thương hiệu '" + request.getName() + "' đã tồn tại.");
        }

        // 2. Dùng Mapper chuyển DTO sang Entity (bỏ qua id và slug)
        Brand brand = brandMapper.toBrand(request);

        // 3. Dùng SlugService để tạo slug từ tên
        String slug = slugService.toSlug(request.getName());
        brand.setSlug(slug);

        // 4. Lưu vào CSDL
        Brand savedBrand = brandRepository.save(brand);
        log.info("Đã tạo thương hiệu mới: {}", savedBrand.getName());

        // 5. Map sang Response DTO và trả về
        return brandMapper.toBrandResponse(savedBrand);
    }

    @Override
    @Transactional(readOnly = true) // Tối ưu hóa cho việc đọc CSDL
    public BrandResponse getBrandById(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với ID: " + id));
        return brandMapper.toBrandResponse(brand);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll().stream()
                .map(brandMapper::toBrandResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BrandResponse updateBrand(Long id, BrandRequest request) {
        // 1. Tìm thương hiệu hiện tại
        Brand existingBrand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với ID: " + id));

        // 2. Kiểm tra logic nghiệp vụ: Tên mới không được trùng với tên khác (ngoại trừ chính nó)
        if (!existingBrand.getName().equals(request.getName())) {
            if (brandRepository.findByName(request.getName()).isPresent()) {
                throw new BadRequestException("Tên thương hiệu '" + request.getName() + "' đã tồn tại.");
            }
            // 3. Nếu tên thay đổi, tạo lại slug
            existingBrand.setSlug(slugService.toSlug(request.getName()));
        }

        // 4. Dùng MapStruct để cập nhật các trường còn lại (name, logoUrl)
        brandMapper.updateBrandFromRequest(request, existingBrand);

        // 5. Lưu và trả về
        Brand updatedBrand = brandRepository.save(existingBrand);
        log.info("Đã cập nhật thương hiệu ID {}: {}", id, updatedBrand.getName());
        
        return brandMapper.toBrandResponse(updatedBrand);
    }

    @Override
    @Transactional
    public void deleteBrand(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy thương hiệu với ID: " + id);
        }

        // TODO: (Module 2) Kiểm tra ràng buộc nghiệp vụ nâng cao
        // (Bỏ comment khi có ProductRepository)
        /*
        if (productRepository.existsByBrandId(id)) {
            throw new BadRequestException("Không thể xóa thương hiệu này. Vẫn còn sản phẩm liên kết.");
        }
        */

        brandRepository.deleteById(id);
        log.info("Đã xóa thương hiệu ID: {}", id);
    }
}