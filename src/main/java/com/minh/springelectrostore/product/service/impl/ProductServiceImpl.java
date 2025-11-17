package com.minh.springelectrostore.product.service.impl;

import com.minh.springelectrostore.product.dto.request.ProductImageRequest; // Thêm import
import com.minh.springelectrostore.product.dto.request.ProductRequest;
import com.minh.springelectrostore.product.dto.request.ProductSearchCriteria;
import com.minh.springelectrostore.product.dto.request.ProductVariantRequest;
import com.minh.springelectrostore.product.dto.response.ProductDetailResponse;
import com.minh.springelectrostore.product.dto.response.ProductSummaryResponse;
import com.minh.springelectrostore.product.entity.Brand;
import com.minh.springelectrostore.product.entity.Category;
import com.minh.springelectrostore.product.entity.Product;
import com.minh.springelectrostore.product.entity.ProductImage;
import com.minh.springelectrostore.product.entity.ProductVariant;
import com.minh.springelectrostore.product.mapper.ProductMapper;
import com.minh.springelectrostore.product.repository.BrandRepository;
import com.minh.springelectrostore.product.repository.CategoryRepository;
import com.minh.springelectrostore.product.repository.ProductRepository;
import com.minh.springelectrostore.product.repository.ProductSpecification;
import com.minh.springelectrostore.product.repository.ProductVariantRepository;
import com.minh.springelectrostore.product.service.ProductService;
import com.minh.springelectrostore.search.event.ProductSyncEvent;
import com.minh.springelectrostore.shared.exception.BadRequestException;
import com.minh.springelectrostore.shared.exception.ResourceNotFoundException;
import com.minh.springelectrostore.shared.util.SlugService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final SlugService slugService;
    private final ApplicationEventPublisher eventPublisher;
    private final ProductVariantRepository productVariantRepository;

    // ... (Các hàm searchProducts, getProductBySlug, getProductById GIỮ NGUYÊN) ...
    @Override
    @Transactional(readOnly = true)
    @Cacheable("products")
    public Page<ProductSummaryResponse> searchProducts(ProductSearchCriteria criteria, Pageable pageable) {
        Specification<Product> spec = new ProductSpecification(criteria);
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        return productPage.map(productMapper::toSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "product_detail", key = "#p0") // Đã sửa key cache ở bước trước
    public ProductDetailResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với slug: " + slug));
        return productMapper.toDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(Long id) {
        Product product = findProductById(id);
        return productMapper.toDetailResponse(product);
    }

    // Hàm createProduct có thể giữ nguyên hoặc refactor dùng chung hàm helper
    @Override
    @Transactional
    public ProductDetailResponse createProduct(ProductRequest request) {
        Category category = findCategoryById(request.getCategoryId());
        Brand brand = findBrandById(request.getBrandId());

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setShortDescription(request.getShortDescription());
        product.setActive(request.isActive());
        product.setSlug(generateUniqueSlug(request.getName()));
        product.setCategory(category);
        product.setBrand(brand);

        // Xử lý Variants cho Create (Dùng cách cũ cũng được vì Create không sợ trùng ID cũ)
        // Nhưng để code sạch, ta dùng hàm helper mới luôn
        updateVariantsSafe(product, request.getVariants());

        Product savedProduct = productRepository.save(product);
        log.info("Đã tạo sản phẩm mới ID {}: {}", savedProduct.getId(), savedProduct.getName());

        eventPublisher.publishEvent(new ProductSyncEvent(this, savedProduct.getId(), ProductSyncEvent.SyncAction.CREATE_UPDATE));
        return productMapper.toDetailResponse(savedProduct);
    }

    // --- SỬA LẠI HÀM NÀY ---
    @Override
    @Transactional
    public ProductDetailResponse updateProduct(Long id, ProductRequest request) {
        Product existingProduct = findProductById(id);

        // 1. Cập nhật thông tin cơ bản
        existingProduct.setName(request.getName());
        existingProduct.setDescription(request.getDescription());
        existingProduct.setShortDescription(request.getShortDescription());
        existingProduct.setActive(request.isActive());

        // 2. Cập nhật Slug nếu tên thay đổi
        if (!existingProduct.getName().equals(request.getName())) {
        	existingProduct.setSlug(generateUniqueSlug(request.getName()));
        }

        // 3. Cập nhật Category và Brand
        if (!existingProduct.getCategory().getId().equals(request.getCategoryId())) {
            Category newCategory = findCategoryById(request.getCategoryId());
            existingProduct.setCategory(newCategory);
        }
        if (!existingProduct.getBrand().getId().equals(request.getBrandId())) {
            Brand newBrand = findBrandById(request.getBrandId());
            existingProduct.setBrand(newBrand);
        }

        // 4. Cập nhật Variants (DÙNG HÀM HELPER MỚI ĐỂ TRÁNH LỖI DUPLICATE SKU)
        updateVariantsSafe(existingProduct, request.getVariants());

        // 5. Lưu
        Product savedProduct = productRepository.save(existingProduct);
        log.info("Đã cập nhật sản phẩm ID {}: {}", savedProduct.getId(), savedProduct.getName());

        // 6. Đồng bộ
        eventPublisher.publishEvent(new ProductSyncEvent(this, savedProduct.getId(), ProductSyncEvent.SyncAction.CREATE_UPDATE));
        
        return productMapper.toDetailResponse(savedProduct);
    }
    // -----------------------

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductById(id);
        product.setActive(false); 
        Product savedProduct = productRepository.save(product);
        log.info("Đã ẩn (xóa mềm) sản phẩm ID {}: {}", id, product.getName());
        eventPublisher.publishEvent(new ProductSyncEvent(this, savedProduct.getId(), ProductSyncEvent.SyncAction.CREATE_UPDATE));
    }

    // --- CÁC HÀM HELPER ---

    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + id));
    }

    private Brand findBrandById(Long brandId) {
        return brandRepository.findById(brandId)
                .orElseThrow(() -> new BadRequestException("Brand ID: " + brandId + " không tồn tại."));
    }

    private Category findCategoryById(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BadRequestException("Category ID: " + categoryId + " không tồn tại."));
    }

    /**
     * HÀM MỚI: Cập nhật danh sách biến thể một cách an toàn (Merge)
     * Tránh lỗi Duplicate Key SKU.
     */
    private void updateVariantsSafe(Product product, List<ProductVariantRequest> variantRequests) {
        if (variantRequests == null || variantRequests.isEmpty()) {
            throw new BadRequestException("Sản phẩm phải có ít nhất 1 biến thể.");
        }

        Set<String> requestSkus = variantRequests.stream()
                .map(ProductVariantRequest::getSku)
                .collect(Collectors.toSet());

        // 1. XÓA biến thể cũ
        product.getVariants().removeIf(existingVariant -> !requestSkus.contains(existingVariant.getSku()));

        // 2. CẬP NHẬT hoặc THÊM MỚI
        for (ProductVariantRequest req : variantRequests) {
            Optional<ProductVariant> existingOpt = product.getVariants().stream()
                    .filter(v -> v.getSku().equals(req.getSku()))
                    .findFirst();

            if (existingOpt.isPresent()) {
                // --- CASE A: ĐÃ CÓ TRONG PRODUCT NÀY -> CẬP NHẬT ---
                ProductVariant existing = existingOpt.get();
                existing.setPrice(req.getPrice());
                existing.setStockQuantity(req.getStockQuantity());
                existing.setColor(req.getColor());
                existing.setStorage(req.getStorage());
                existing.setRam(req.getRam());
                updateVariantImages(existing, req.getImages());
            } else {
                // --- CASE B: CHƯA CÓ -> THÊM MỚI ---
                
                // [QUAN TRỌNG] Kiểm tra xem SKU này đã tồn tại ở sản phẩm KHÁC chưa?
                // Nếu đã tồn tại trong DB -> Báo lỗi User ngay lập tức
                if (productRepository.count() > 0) { // Chỉ check khi DB không rỗng
                     // Lưu ý: Cần dùng productVariantRepository để check
                     // Bạn có thể autowire ProductVariantRepository vào class này nếu chưa có access direct
                     // Tuy nhiên, trong code mẫu bạn gửi không thấy field productVariantRepository
                     // Giả sử bạn đã inject nó, hoặc dùng productRepository check gián tiếp (khó hơn)
                }

                if (productVariantRepository.existsBySku(req.getSku())) {
                    throw new BadRequestException("Mã SKU '" + req.getSku() + "' đã tồn tại trên hệ thống.");
               }

               ProductVariant newVariant = new ProductVariant();
                newVariant.setProduct(product);
                newVariant.setSku(req.getSku());
                newVariant.setPrice(req.getPrice());
                newVariant.setStockQuantity(req.getStockQuantity());
                newVariant.setColor(req.getColor());
                newVariant.setStorage(req.getStorage());
                newVariant.setRam(req.getRam());
                
                updateVariantImages(newVariant, req.getImages());
                product.getVariants().add(newVariant);
            }
        }
    }

    // Hàm helper xử lý ảnh
    private void updateVariantImages(ProductVariant variant, List<ProductImageRequest> imageRequests) {
        if (variant.getImages() == null) {
            variant.setImages(new HashSet<>());
        }
        // Xóa ảnh cũ
        variant.getImages().clear();
        
        if (imageRequests != null) {
            for (ProductImageRequest imgReq : imageRequests) {
                ProductImage image = new ProductImage();
                image.setProductVariant(variant); // Liên kết
                image.setImageUrl(imgReq.getImageUrl());
                image.setThumbnail(imgReq.isThumbnail());
                variant.getImages().add(image);
            }
        }
    }
    
    private String generateUniqueSlug(String name) {
        String baseSlug = slugService.toSlug(name);
        String uniqueSlug = baseSlug;
        int count = 1;
        
        // Vòng lặp kiểm tra xem slug đã tồn tại chưa, nếu có thì thêm số đếm
        while (productRepository.findBySlug(uniqueSlug).isPresent()) {
            uniqueSlug = baseSlug + "-" + count;
            count++;
        }
        return uniqueSlug;
    }
}