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

        userId = "11";
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


    // 개인 신원 인증 (JWT 토큰의 이름 vs 입력한 이름)
    @PostMapping("/verify-personal")
    public ResponseEntity<Map<String, Object>> verifyPersonalIdentity(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {

        log.info("개인 신원 인증 요청: {}", request);

        String personalName = request.get("personalName");
        if (personalName == null || personalName.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "message", "개인 이름을 입력해주세요"
                ));
        }

        try {
            // Authorization 헤더에서 JWT 토큰 추출
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", "로그인이 필요합니다"
                    ));
            }

            String token = authHeader.substring(7);

            // JWT 토큰에서 이름 추출
            var claims = jwtUtil.parse(token);
            String jwtName = JwtClaimAccessor.name(claims);

            if (jwtName == null || jwtName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "message", "토큰에서 이름 정보를 찾을 수 없습니다"
                    ));
            }

            // 이름 비교 (정규화 후)
            String normalizedJwtName = jwtName.trim().replaceAll("\\s+", "").toLowerCase();
            String normalizedInputName = personalName.trim().replaceAll("\\s+", "").toLowerCase();

            boolean isVerified = normalizedJwtName.equals(normalizedInputName);

            log.info("개인 신원 인증 결과: {} (JWT: '{}' vs 입력: '{}')",
                isVerified ? "성공" : "실패", jwtName, personalName);

            return ResponseEntity.ok(Map.of(
                "success", isVerified,
                "message", isVerified ? "신원 인증이 완료되었습니다" : "입력하신 이름이 등록된 정보와 일치하지 않습니다",
                "jwtName", jwtName,
                "inputName", personalName
            ));

        } catch (Exception e) {
            log.error("개인 신원 인증 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "message", "인증 처리 중 오류가 발생했습니다: " + e.getMessage()
                ));
        }
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

}
