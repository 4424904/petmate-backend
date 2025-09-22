package com.petmate.domain.product.controller;

import com.petmate.domain.company.service.CompanyService;
import com.petmate.domain.company.dto.response.CompanyResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyListController {

    private final CompanyService companyService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getCompanies() {
        log.info("업체 목록 조회 요청 (실제 DB 데이터)");

        try {
            // 실제 DB에서 승인된 업체들 조회
            List<CompanyResponseDto> allCompanies = companyService.getAllApprovedCompanies();

            // 상품 등록에 필요한 필드만 추출
            List<Map<String, Object>> companies = allCompanies.stream()
                    .map(this::createCompanyForProduct)
                    .collect(Collectors.toList());

            log.info("실제 업체 {} 개 반환", companies.size());
            return ResponseEntity.ok(companies);
        } catch (Exception e) {
            log.error("업체 목록 조회 중 오류 발생:", e);

            // 오류 발생 시 빈 목록 반환
            return ResponseEntity.ok(List.of());
        }
    }

    private Map<String, Object> createCompanyForProduct(CompanyResponseDto company) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", company.getId());
        result.put("name", company.getName());
        result.put("services", company.getServices()); // 서비스 정보 포함
        result.put("repService", company.getRepService()); // 대표 서비스 포함

        log.info("업체 정보 - id: {}, name: {}, services: {}, repService: {}",
                company.getId(), company.getName(), company.getServices(), company.getRepService());

        return result;
    }
}
