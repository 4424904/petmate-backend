package com.petmate.domain.company.controller;

import com.petmate.domain.company.dto.request.CompanyRegisterRequestDto;
import com.petmate.domain.company.dto.request.CompanyUpdateRequestDto;
import com.petmate.domain.company.dto.response.BusinessInfoResponseDto;
import com.petmate.domain.company.dto.response.CompanyResponseDto;
import com.petmate.domain.company.service.CompanyService;
import com.petmate.security.jwt.JwtUtil;
import com.petmate.security.jwt.JwtClaimAccessor;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtUtil jwtUtil;

    // 업체 등록
    @PostMapping("/register")
    public ResponseEntity<?> registerCompany(
            @ModelAttribute CompanyRegisterRequestDto requestDto,
            @AuthenticationPrincipal String userId) {

        log.info("=== 컨트롤러 진입 ===");
        log.info("업체 등록 요청 - userId: {}, companyType: {}", userId, requestDto.getType());

        try {
            CompanyResponseDto company = companyService.registerCompany(requestDto, Integer.parseInt(userId));
            return ResponseEntity.ok(company);
        } catch (IllegalArgumentException e) {
            log.error("업체 등록 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "message", e.getMessage()
                ));
        } catch (Exception e) {
            log.error("업체 등록 중 예상치 못한 오류: ", e);
            return ResponseEntity.status(500)
                .body(Map.of(
                    "success", false,
                    "message", "서버 오류가 발생했습니다: " + e.getMessage()
                ));
        }
    }

    // 내가 등록한 업체 목록 조회
    @GetMapping("/my")
    public ResponseEntity<List<CompanyResponseDto>> getMyCompanies(
            @AuthenticationPrincipal String userId) {

        log.info("내 업체 목록 조회 요청 - userId: {}", userId);

        List<CompanyResponseDto> companies = companyService.getMyCompanies(Integer.parseInt(userId));
        return ResponseEntity.ok(companies);
    }

    // 업체 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<CompanyResponseDto> getCompany(
            @PathVariable Integer id,
            @AuthenticationPrincipal String userId) {

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

        log.info("업체 수정 요청 - companyId: {}, userId: {}", id, userId);

        CompanyResponseDto company = companyService.updateCompany(id, requestDto, Integer.parseInt(userId));
        return ResponseEntity.ok(company);
    }

    // 업체 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCompany(
            @PathVariable Integer id,
            @AuthenticationPrincipal String userId) {

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

        String status = request.get("status");
        log.info("업체 상태 변경 요청 - companyId: {}, status: {}, userId: {}", id, status, userId);

        CompanyResponseDto company = companyService.updateCompanyStatus(id, status, Integer.parseInt(userId));
        return ResponseEntity.ok(company);
    }


    // 사업자등록번호 중복 체크 (DB 기반)
    @PostMapping("/get-business-info")
    public ResponseEntity<BusinessInfoResponseDto> getBusinessInfo(
            @RequestBody Map<String, String> request) {

        log.info("사업자등록번호 중복 체크 요청: {}", request);

        String businessNumber = request.get("businessNumber");
        if (businessNumber == null || businessNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(BusinessInfoResponseDto.builder()
                    .businessNumber("")
                    .isValid(false)
                    .message("사업자등록번호를 입력해주세요")
                    .build());
        }

        // 숫자만 추출
        String cleanBusinessNumber = businessNumber.replaceAll("[^0-9]", "");
        if (cleanBusinessNumber.length() != 10) {
            return ResponseEntity.badRequest()
                .body(BusinessInfoResponseDto.builder()
                    .businessNumber(cleanBusinessNumber)
                    .isValid(false)
                    .message("사업자등록번호는 10자리 숫자여야 합니다")
                    .build());
        }

        BusinessInfoResponseDto result = companyService.getBusinessInfo(cleanBusinessNumber);
        return ResponseEntity.ok(result);
    }

    // 개인 업체 등록 여부 확인 (createdBy 기반)
    @GetMapping("/check-personal-exists")
    public ResponseEntity<Map<String, Object>> checkPersonalCompanyExists(
            @AuthenticationPrincipal String userId) {

        log.info("개인 업체 중복 확인 요청 - userId: {}", userId);

        try {
            boolean exists = companyService.checkPersonalCompanyExists(Integer.parseInt(userId));

            return ResponseEntity.ok(Map.of(
                "exists", exists,
                "message", exists ? "이미 등록된 개인 업체가 있습니다." : "개인 업체 등록이 가능합니다."
            ));

        } catch (Exception e) {
            log.error("개인 업체 중복 확인 중 오류 발생: ", e);
            return ResponseEntity.status(500)
                .body(Map.of(
                    "exists", true,
                    "message", "확인 중 오류가 발생했습니다: " + e.getMessage()
                ));
        }
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<CompanyResponseDto>> getNearbyCompanies(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5.0") Double radius,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String keyword
    ) {
        List<CompanyResponseDto> companies = companyService.getNearbyCompanies(latitude, longitude, radius, serviceType, keyword);

        return ResponseEntity.ok(companies);
    }

    // 업체별 제공 서비스 유형 조회
    @GetMapping("/{id}/service-types")
    public ResponseEntity<List<String>> getCompanyServiceTypes(@PathVariable Integer id) {
        log.info("업체별 서비스 유형 조회 요청 - companyId: {}", id);

        List<String> serviceTypes = companyService.getCompanyServiceTypes(id);
        return ResponseEntity.ok(serviceTypes);
    }

    // 공개 업체 정보 조회 (인증 불필요, 예약에서 사용)
    @GetMapping("/public/{id}")
    public ResponseEntity<CompanyResponseDto> getCompanyByIdPublic(@PathVariable Integer id) {
        log.info("공개 업체 정보 조회 요청 - companyId: {}", id);

        try {
            CompanyResponseDto company = companyService.getCompanyByIdPublic(id);
            return ResponseEntity.ok(company);
        } catch (IllegalArgumentException e) {
            log.error("업체 조회 실패: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("업체 조회 중 오류 발생", e);
            return ResponseEntity.status(500).build();
        }
    }

}
