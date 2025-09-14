package com.petmate.domain.product.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/companies")
@RequiredArgsConstructor
public class CompanyListController {

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getCompanies() {
        log.info("업체 목록 조회 요청");

        // 하드코딩된 업체 목록 (나중에 DB로 변경 가능)
        List<Map<String, Object>> companies = Arrays.asList(
                createCompany(1, "김밥시티 개인사업"),
                createCompany(2, "행복한 돌봄 센터"),
                createCompany(3, "편리한 생활 서비스"),
                createCompany(4, "따뜻한 케어"),
                createCompany(5, "펫메이트 본사")
        );

        log.info("업체 {} 개 반환", companies.size());
        return ResponseEntity.ok(companies);
    }

    private Map<String, Object> createCompany(Integer id, String name) {
        Map<String, Object> company = new HashMap<>();
        company.put("id", id);
        company.put("name", name);
        return company;
    }
}
