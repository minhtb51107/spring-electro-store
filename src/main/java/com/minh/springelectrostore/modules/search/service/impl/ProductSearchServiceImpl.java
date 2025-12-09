package com.minh.springelectrostore.modules.search.service.impl;

import com.minh.springelectrostore.modules.product.dto.request.ProductSearchCriteria;
import com.minh.springelectrostore.modules.product.dto.response.ProductSummaryResponse;
import com.minh.springelectrostore.modules.search.document.ProductDocument;
import com.minh.springelectrostore.modules.search.event.ProductSyncEvent;
import com.minh.springelectrostore.modules.search.mapper.ProductSearchMapper;
import com.minh.springelectrostore.modules.search.repository.ProductSearchRepository;
import com.minh.springelectrostore.modules.search.service.ProductSearchService;
import com.minh.springelectrostore.modules.search.service.ProductSyncService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchServiceImpl implements ProductSearchService {

    private final ProductSearchRepository productSearchRepository;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ProductSearchMapper productSearchMapper;
    
    // Inject service sync để xử lý các sự kiện đồng bộ
    private final ProductSyncService productSyncService;

    @Override
    public void handleProductSyncEvent(ProductSyncEvent event) {
        productSyncService.handleProductSyncEvent(event);
    }

    @Override
    public void deleteProductFromIndex(Long productId) {
        productSyncService.deleteProductFromIndex(productId);
    }

    @Override
    public void indexProduct(Long productId) {
        productSyncService.indexProduct(productId);
    }

    @Override
    public Page<ProductSummaryResponse> searchProducts(String keyword, ProductSearchCriteria criteria, Pageable pageable) {
        log.info("Searching ES keyword='{}', criteria={}", keyword, criteria);

        BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();

        // 1. Filter: Chỉ lấy sản phẩm đang active
        boolQueryBuilder.filter(q -> q.term(t -> t.field("active").value(true)));

        // 2. Filter: Category (Exact match)
        // [FIX] Sử dụng getCategorySlug() (số ít) từ DTO của bạn
        if (criteria != null && StringUtils.hasText(criteria.getCategorySlug())) {
             boolQueryBuilder.filter(q -> q.term(t -> t.field("categorySlug").value(criteria.getCategorySlug())));
        }

        // 3. Filter: Brand (Exact match)
        // [FIX] Sử dụng getBrandSlug() từ DTO
        if (criteria != null && StringUtils.hasText(criteria.getBrandSlug())) {
            boolQueryBuilder.filter(q -> q.term(t -> t.field("brandSlug").value(criteria.getBrandSlug())));
        }

        // 4. Filter: Price Range
        if (criteria != null && (criteria.getPriceGte() != null || criteria.getPriceLte() != null)) {
            boolQueryBuilder.filter(q -> q.range(r -> r
                .number(n -> { 
                    n.field("price");
                    if (criteria.getPriceGte() != null) n.gte(criteria.getPriceGte().doubleValue());
                    if (criteria.getPriceLte() != null) n.lte(criteria.getPriceLte().doubleValue());
                    return n;
                })
            ));
        }

        // 5. Full-text search (Tìm kiếm theo từ khóa)
        if (!StringUtils.hasText(keyword)) {
            // Nếu không có từ khóa -> Lấy tất cả (matchAll)
            boolQueryBuilder.must(q -> q.matchAll(m -> m));
        } else {
            // Nếu có từ khóa -> Tìm trên nhiều trường (Tên, Mô tả, Danh mục, Thương hiệu)
            // Tên sản phẩm được ưu tiên (boost ^3)
            boolQueryBuilder.must(q -> q.multiMatch(mm -> mm
                    .query(keyword)
                    .fields("name^3", "description", "categoryName", "brandName")
                    .fuzziness("AUTO") // Tự động sửa lỗi chính tả nhẹ
            ));
        }

        // Build Query
        Query esQuery = Query.of(q -> q.bool(boolQueryBuilder.build()));

        NativeQuery query = new NativeQueryBuilder()
                .withQuery(esQuery)
                .withPageable(pageable)
                .build();

        // Execute Search
        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);

        // Map kết quả sang DTO
        List<ProductSummaryResponse> results = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(productSearchMapper::toSummaryResponse)
                .collect(Collectors.toList());

        // Trả về Page
        return PageableExecutionUtils.getPage(
                results,
                pageable,
                searchHits::getTotalHits
        );
    }
}