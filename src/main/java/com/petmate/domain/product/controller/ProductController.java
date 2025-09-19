package com.petmate.domain.product.controller;

import com.petmate.domain.company.dto.response.CompanyResponseDto;
import com.petmate.domain.company.service.CompanyService;
import com.petmate.domain.product.dto.request.ProductCreateRequest;
import com.petmate.domain.product.dto.request.ProductRequestDto;
import com.petmate.domain.product.dto.request.ProductSearchRequest;
import com.petmate.domain.product.dto.request.ProductUpdateRequest;
import com.petmate.domain.product.dto.response.ProductResponseDto;
import com.petmate.domain.product.entity.ProductEntity;
import com.petmate.domain.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductService productService;
    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getAllProducts(@AuthenticationPrincipal String userId) {
        List<ProductResponseDto> responses = productService.getAllProducts(userId);
        return ResponseEntity.ok(responses);
    }

    // 내 업체 목록 조회(상품 등록시 사용)
    @GetMapping("/companies")
    public ResponseEntity<List<Map<String, Object>>> getMyCompaniesForProduct(
            @AuthenticationPrincipal String userEmail) {

        log.info("업체 목록 조회 요청 - userEmail: {}", userEmail);
        Integer actualUserId = productService.getUserIdByEmail(userEmail);
        log.info("실제 userId: {}", actualUserId);

        List<CompanyResponseDto> companies = companyService.getMyCompanies(actualUserId);

        List<Map<String, Object>> response = companies.stream()
                .map(company -> {
                    Map<String, Object> companyMap = new HashMap<>();
                    companyMap.put("id", company.getId());
                    companyMap.put("name", company.getName());
                    return companyMap;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);

    }


    // 상품 생성
    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        ProductResponseDto response = productService.createProduct(request);
        return ResponseEntity.ok(response);
    }
    
    // 상품 단건 조회
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable Integer id) {
        ProductResponseDto response = productService.getProduct(id);
        return ResponseEntity.ok(response);
    }

    // 상품 검색
    @GetMapping("/search")
    public ResponseEntity<List<ProductResponseDto>> searchProducts(@ModelAttribute ProductSearchRequest request) {
        List<ProductEntity> products = productService.searchProducts(request);
        List<ProductResponseDto> responses = products.stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // 업체별 상품 조회
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<ProductResponseDto>> getProductsByCompany(@PathVariable Integer companyId) {
        List<ProductEntity> products = productService.getProductsByCompany(companyId);
        List<ProductResponseDto> responses = products.stream()
                .map(ProductResponseDto::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // 상품 수정
    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDto> updateProduct(@PathVariable Integer id, @Valid @RequestBody ProductUpdateRequest productUpdateRequest) {
        ProductResponseDto productResponseDto = productService.updateRepository(id, productUpdateRequest);
        return ResponseEntity.ok(productResponseDto);
    }

    // 상품 삭제 전 확인
    @GetMapping("/{id}/deletion-check")
    public ResponseEntity<Map<String, Object>> checkProductDeletion(@PathVariable Integer id) {
        Map<String, Object> result = productService.checkProductDeletion(id);
        return ResponseEntity.ok(result);
    }

    // 슬롯과 함께 상품 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProductWithSlots(@PathVariable Integer id,
            @RequestParam(defaultValue = "true") boolean deleteSlots) {
        productService.deleteProductWithSlots(id, deleteSlots);
        return ResponseEntity.noContent().build();
    }

    // ProductController.java 파일 맨 아래에 추가
    @GetMapping("/service-categories")
    public ResponseEntity<List<Map<String, Object>>> getServiceCategories() {
        log.info("서비스 카테고리 목록 조회 요청");

        List<Map<String, Object>> categories = Arrays.asList(
                createCategory("C", "돌봄"),
                createCategory("W", "산책"),
                createCategory("G", "미용"),
                createCategory("M", "병원"),
                createCategory("E", "기타")
        );

        return ResponseEntity.ok(categories);
    }

    private Map<String, Object> createCategory(String id, String name) {
        Map<String, Object> category = new HashMap<>();
        category.put("id", id);
        category.put("name", name);
        return category;
    }
}
