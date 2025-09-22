package com.petmate.domain.product.repository.mybatis;

import com.petmate.domain.product.dto.request.ProductRequestDto;
import com.petmate.domain.product.dto.request.ProductSearchRequest;
import com.petmate.domain.product.entity.ProductEntity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ProductMapper {

    // 복잡한 검색 쿼리
    List<ProductEntity> searchProducts(ProductSearchRequest productSearchRequest);

    // 통계 쿼리
    List<Map<String, Object>> getProductStatsByCompany(Integer companyId);

    // 가격대별 상품 수
    List<Map<String, Object>> getProductCountByPriceRange();

    // 인기 상품 (예약 수 기반)
    List<ProductEntity> getPopularProducts(int limit);


}
