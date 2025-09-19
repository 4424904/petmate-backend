package com.petmate.domain.product.service;

import com.petmate.domain.company.dto.response.CompanyResponseDto;
import com.petmate.domain.company.service.CompanyService;
import com.petmate.domain.product.dto.request.ProductCreateRequest;
import com.petmate.domain.product.dto.request.ProductSearchRequest;
import com.petmate.domain.product.dto.request.ProductUpdateRequest;
import com.petmate.domain.product.dto.response.ProductResponseDto;
import com.petmate.domain.product.entity.ProductEntity;
import com.petmate.domain.product.repository.jpa.ProductRepository;
import com.petmate.domain.product.repository.mybatis.ProductMapper;
import com.petmate.domain.user.entity.UserEntity;
import com.petmate.domain.user.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
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
    private final AvailabilitySlotService slotService;
    private final CompanyService companyService;
    private final UserRepository userRepository;

    // 전체상품조회(사용자별)
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts(String userEmail) {
        try {
            log.info("=== getAllProducts 호출 시작 ===");
            log.info("userEmail: {}", userEmail);

            // 이메일로 실제 userId 조회
            Integer actualUserId = getUserIdByEmail(userEmail);
            log.info("실제 userId: {}", actualUserId);

            // 사용자가 등록한 업체 ID 목록 조회
            List<CompanyResponseDto> myCompanies = companyService.getMyCompanies(actualUserId);
            log.info("조회된 업체 수: {}", myCompanies.size());

            // CompanyResponseDto -> ID 가져오기
            List<Integer> myCompanyIds = myCompanies.stream()
                    .map(CompanyResponseDto::getId)
                    .toList();
            log.info("업체 ID 목록: {}", myCompanyIds);

            // 해당 업체 상품 조회
            List<ProductEntity> products;
            if(myCompanyIds.isEmpty()) {
                log.info("등록된 업체가 없음 - 빈 목록 반환");
                products = new ArrayList<>();
            } else {
                products = productRepository.findByCompanyIdIn(myCompanyIds);
                log.info("조회된 상품 수: {}", products.size());
            }

            List<ProductResponseDto> result = products.stream()
                    .map(ProductResponseDto::from)
                    .collect(Collectors.toList());

            log.info("=== getAllProducts 호출 완료 ===");
            return result;

        } catch (Exception e) {
            log.error("getAllProducts 에러:", e);
            throw e;
        }
    }

    // 이메일로 userId 조회 헬퍼 메서드
    public Integer getUserIdByEmail(String email) {
        log.info("이메일로 사용자 조회: {}", email);

        try {
            UserEntity user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

            Integer userId = user.getId().intValue();
            log.info("사용자 조회 성공 - ID: {}, 닉네임: {}", userId, user.getNickName());

            return userId;
        } catch (Exception e) {
            log.error("getUserIdByEmail 실패:", e);
            throw new RuntimeException("사용자 조회 실패: " + email, e);
        }
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


    // 상품 수정
    @Transactional
    public ProductResponseDto updateRepository(Integer id, ProductUpdateRequest productUpdateRequest) {
        log.info("상품 수정 시작 : {}" , id);
        log.info("수정데이터 {}", productUpdateRequest);
        log.info("수정 요청 minPet: {}, maxPet: {}", productUpdateRequest.getMinPet(), productUpdateRequest.getMaxPet());

        // 기존 상품 조회
        ProductEntity existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다." + id));

        log.info("기존 상품 minPet: {}, maxPet: {}", existingProduct.getMinPet(), existingProduct.getMaxPet());

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

        log.info("수정 후 Entity minPet: {}, maxPet: {}", existingProduct.getMinPet(), existingProduct.getMaxPet());

        // jpa로 저장
        ProductEntity updatedProduct = productRepository.save(existingProduct);
        log.info("저장된 Product minPet: {}, maxPet: {}", updatedProduct.getMinPet(), updatedProduct.getMaxPet());
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

    // 상품 삭제 전 확인 (슬롯 정보 포함)
    @Transactional(readOnly = true)
    public Map<String, Object> checkProductDeletion(Integer id) {
        log.info("상품 삭제 전 확인 시작: {}", id);

        // 상품 존재 확인
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + id));

        // 슬롯 정보 조회
        Map<String, Object> slotInfo = slotService.getProductSlotInfo(id);

        Map<String, Object> result = new HashMap<>();
        result.put("product", ProductResponseDto.from(product));
        result.put("slotInfo", slotInfo);

        log.info("상품 삭제 확인 완료: {} - 슬롯 정보: {}", id, slotInfo);
        return result;
    }

    // 슬롯과 함께 상품 삭제
    @Transactional
    public void deleteProductWithSlots(Integer id, boolean deleteSlots) {
        log.info("슬롯과 함께 상품 삭제 시작: {} - 슬롯 삭제: {}", id, deleteSlots);

        // 상품 존재 확인
        ProductEntity product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + id));

        if (deleteSlots) {
            // 예약된 슬롯이 있는지 확인
            Map<String, Object> slotInfo = slotService.getProductSlotInfo(id);
            Long bookedSlots = (Long) slotInfo.get("bookedSlots");

            if (bookedSlots > 0) {
                log.warn("예약된 슬롯이 있어서 삭제 불가: {} - 예약된 슬롯: {}개", id, bookedSlots);
                throw new RuntimeException("예약된 슬롯이 있어서 삭제할 수 없습니다. 예약된 슬롯: " + bookedSlots + "개");
            }

            // 슬롯 먼저 삭제
            slotService.deleteSlotsByProductId(id);
            log.info("상품 {}의 모든 슬롯 삭제 완료", id);
        }

        // 상품 삭제
        productRepository.delete(product);
        log.info("상품 삭제 완료: {}", id);
    }


}
