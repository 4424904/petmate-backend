package com.petmate.domain.company.controller;

import com.petmate.domain.company.dto.response.BusinessData;
import com.petmate.domain.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
@Slf4j
public class BusinessController {

    private final CompanyService companyService;

    @PostMapping("/check")
    public ResponseEntity<BusinessData> checkBusinessNumber(@RequestBody Map<String, String> request) {
        String businessNumber = request.get("businessNumber");
        log.info("사업자등록번호 조회 요청 - businessNumber: {}", businessNumber);

        BusinessData result = companyService.checkBusinessNumber(businessNumber);
        return ResponseEntity.ok(result);
    }
}