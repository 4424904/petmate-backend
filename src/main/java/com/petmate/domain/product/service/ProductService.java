package com.petmate.domain.product.service;

import com.petmate.domain.product.dto.request.ProductCreateRequest;
import com.petmate.domain.product.dto.request.ProductRequestDto;
import com.petmate.domain.product.dto.request.ProductSearchRequest;
import com.petmate.domain.product.dto.request.ProductUpdateRequest;
import com.petmate.domain.product.dto.response.ProductResponseDto;
import com.petmate.domain.product.entity.ProductEntity;
import com.petmate.domain.product.repository.jpa.ProductRepository;
import com.petmate.domain.product.repository.mybatis.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    // 전체상품조회
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        List<ProductEntity> products = productRepository.findAll();
        return products.stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
    }

    // 상품 생성
    @Transactional
    public ProductResponseDto createProduct(ProductCreateRequest request) {

        log.info("상품생성시작");
        log.info("받은 데이터 :{}", request);

        ProductEntity productEntity = ProductEntity.builder()
                .companyId(request.getCompanyId())
                .serviceType(request.getServiceType())
                .name(request.getName())
                .price(request.getPrice())
                .allDay(request.getAllDay())
                .durationMin(request.getDurationMin())
                .introText(request.getIntroText())
                .minPet(request.getMinPet())
                .maxPet(request.getMaxPet())
                .isActive(request.getIsActive())
                .build();

        log.info("Entity 생성 ", productEntity);
        ProductEntity savedEntity = productRepository.save(productEntity);
        log.info("Entity 저장 ", savedEntity);

        return ProductResponseDto.from(savedEntity);
    }

    // 상품 조회
    @Transactional(readOnly = true)
    public ProductResponseDto getProduct(Integer id) {
        ProductEntity productEntity = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        return ProductResponseDto.from(productEntity);
    }

    // 복잡한 검색
    @Transactional(readOnly = true)
    public List<ProductEntity> searchProducts(ProductSearchRequest productSearchRequest) {
        return productMapper.searchProducts(productSearchRequest);
    }

    // 단순 조회
    @Transactional(readOnly = true)
    public List<ProductEntity> getProductsByCompany(Integer companyId) {
        return productRepository.findActiveProductsByCompany(companyId);
    }

    // 통계
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProductStats(Integer companyId) {
        return productMapper.getProductStatsByCompany(companyId);
    }

    // 상품 수정
    public ProductResponseDto updateRepository(Integer id, ProductUpdateRequest productUpdateRequest) {
        log.info("상품 수정 시작 : {}" , id);
        log.info("수정데이터 {}", productUpdateRequest);

        // 기존 상품 조회
        ProductEntity existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다." + id));

        log.info("기존 상품 ? {} ", existingProduct);

        // 업데이트
        existingProduct.setCompanyId(productUpdateRequest.getCompanyId());
        existingProduct.setServiceType(productUpdateRequest.getServiceType());
        existingProduct.setName(productUpdateRequest.getName());
        existingProduct.setPrice(productUpdateRequest.getPrice());
        existingProduct.setAllDay(productUpdateRequest.getAllDay());
        existingProduct.setDurationMin(productUpdateRequest.getDurationMin());
        existingProduct.setIntroText(productUpdateRequest.getIntroText());
        existingProduct.setMinPet(productUpdateRequest.getMinPet());
        existingProduct.setMaxPet(productUpdateRequest.getMaxPet());
        existingProduct.setIsActive(productUpdateRequest.getIsActive());

        // jpa로 저장
        ProductEntity updatedProduct = productRepository.save(existingProduct);
        log.info("수정 완료!!! {}", updatedProduct);

        return  ProductResponseDto.from(updatedProduct);
    }

    // 상품 삭제
    @Transactional
    public void deleteProduct(Integer id) {
        log.info("상품 삭제 {}" , id);

        // 존재 여부 확인(검증하기)
        ProductEntity productEntity = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("삭제할 상품을 찾을 수 없음." + id));

        // 실제 삭제
        productRepository.delete(productEntity);
        log.info("삭제 완료 {}" , id);
    }

}
