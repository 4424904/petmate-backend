package com.petmate.domain.company.controller;


import com.petmate.domain.company.dto.request.CompanyRegisterRequestDto;
import com.petmate.domain.company.dto.request.CompanyUpdateRequestDto;
import com.petmate.domain.company.dto.response.CompanyResponseDto;
import com.petmate.domain.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/company")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {

    private final CompanyService companyService;

    // 업체 등록
    @PostMapping("/register")
    public ResponseEntity<CompanyResponseDto> registerCompany(
            @ModelAttribute CompanyRegisterRequestDto requestDto,
            @AuthenticationPrincipal String userId) {


        userId = "11";
        log.info("=== 컨트롤러 진입 ===");
        log.info("업체 등록 요청 - userId: {}, companyType: {}", userId, requestDto.getType());

        CompanyResponseDto company = companyService.registerCompany(requestDto, Integer.parseInt(userId));
        return ResponseEntity.ok(company);
    }

    // 내가 등록한 업체 목록 조회
    @GetMapping("/my")
    public ResponseEntity<List<CompanyResponseDto>> getMyCompanies(
            @AuthenticationPrincipal String userId) {

        userId = "11";
        log.info("내 업체 목록 조회 요청 - userId: {}", userId);

        List<CompanyResponseDto> companies = companyService.getMyCompanies(Integer.parseInt(userId));
        return ResponseEntity.ok(companies);
    }

    // 업체 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponseDto> getCompany(
            @PathVariable Integer id,
            @AuthenticationPrincipal String userId) {

        userId = "11";
        log.info("업체 상세 조회 요청 - companyId: {}, userId: {}", id, userId);

        CompanyResponseDto company = companyService.getCompanyById(id, Integer.parseInt(userId));
        return ResponseEntity.ok(company);
    }

    // 업체 정보 수정
    @PutMapping("/{id}")
    public ResponseEntity<CompanyResponseDto> updateCompany(
            @PathVariable Integer id,
            @ModelAttribute CompanyUpdateRequestDto requestDto,
            @AuthenticationPrincipal String userId) {

        userId = "11";
        log.info("업체 수정 요청 - companyId: {}, userId: {}", id, userId);

        CompanyResponseDto company = companyService.updateCompany(id, requestDto, Integer.parseInt(userId));
        return ResponseEntity.ok(company);
    }

    // 업체 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(
            @PathVariable Integer id,
            @AuthenticationPrincipal String userId) {

        userId = "11";
        log.info("업체 삭제 요청 - companyId: {}, userId: {}", id, userId);

        companyService.deleteCompany(id, Integer.parseInt(userId));
        return ResponseEntity.ok().build();
    }

    // 업체 승인 상태 변경
    @PutMapping("/{id}/status")
    public ResponseEntity<CompanyResponseDto> updateCompanyStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal String userId) {

        userId = "11";
        String status = request.get("status");
        log.info("업체 상태 변경 요청 - companyId: {}, status: {}, userId: {}", id, status, userId);

        CompanyResponseDto company = companyService.updateCompanyStatus(id, status, Integer.parseInt(userId));
        return ResponseEntity.ok(company);
    }






}
