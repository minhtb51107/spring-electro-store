package com.minh.springelectrostore.search.service.impl;

import com.minh.springelectrostore.product.dto.request.ProductSearchCriteria;
import com.minh.springelectrostore.product.dto.response.ProductSummaryResponse;
import com.minh.springelectrostore.search.document.ProductDocument;
import com.minh.springelectrostore.search.mapper.ProductSearchMapper;
import com.minh.springelectrostore.search.repository.ProductSearchRepository;
import com.minh.springelectrostore.search.service.ProductSearchService;

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

    @Override
    public Page<ProductSummaryResponse> searchProducts(String keyword, ProductSearchCriteria criteria, Pageable pageable) {
        log.info("Đang tìm kiếm Elasticsearch với keyword: '{}' và criteria: {}", keyword, criteria);

        // Build Bool Query
        BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();

        // 1. Filter: active = true
        boolQueryBuilder.filter(q -> q.term(t -> t.field("active").value(true)));

        // 2. Filter: categorySlug (Exact match)
        if (StringUtils.hasText(criteria.getCategorySlug())) {
            boolQueryBuilder.filter(q -> q.term(t -> t.field("categorySlug").value(criteria.getCategorySlug())));
        }

        // 3. Filter: brandSlug (Exact match)
        if (StringUtils.hasText(criteria.getBrandSlug())) {
            boolQueryBuilder.filter(q -> q.term(t -> t.field("brandSlug").value(criteria.getBrandSlug())));
        }

        // 4. Filter: Price Range (ĐÃ SỬA LỖI - Dùng Fluent Builder chuẩn)
        if (criteria.getPriceGte() != null || criteria.getPriceLte() != null) {
            boolQueryBuilder.filter(q -> q.range(r -> r
                .number(n -> { 
                    // Chỉ định trường cần lọc
                    n.field("price");
                    
                    // Thêm điều kiện GTE (Lớn hơn hoặc bằng)
                    if (criteria.getPriceGte() != null) {
                        n.gte(criteria.getPriceGte().doubleValue());
                    }
                    
                    // Thêm điều kiện LTE (Nhỏ hơn hoặc bằng)
                    if (criteria.getPriceLte() != null) {
                        n.lte(criteria.getPriceLte().doubleValue());
                    }
                    return n;
                })
            ));
        }

        // 5. Full-text search
        if (!StringUtils.hasText(keyword)) {
            boolQueryBuilder.must(q -> q.matchAll(m -> m));
        } else {
            boolQueryBuilder.must(q -> q.multiMatch(mm -> mm
                    .query(keyword)
                    .fields("name^3", "description", "categoryName", "brandName")
                    .fuzziness("AUTO")
            ));
        }

        // Build final query
        Query esQuery = Query.of(q -> q.bool(boolQueryBuilder.build()));

        NativeQuery query = new NativeQueryBuilder()
                .withQuery(esQuery)
                .withPageable(pageable)
                .build();

        SearchHits<ProductDocument> searchHits = elasticsearchOperations.search(query, ProductDocument.class);

        List<ProductSummaryResponse> results = searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(productSearchMapper::toSummaryResponse)
                .collect(Collectors.toList());

        return PageableExecutionUtils.getPage(
                results,
                pageable,
                searchHits::getTotalHits
        );
    }
}