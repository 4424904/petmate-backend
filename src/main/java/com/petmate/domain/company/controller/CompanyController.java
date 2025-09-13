package com.petmate.domain.company.controller;


import com.petmate.domain.company.dto.request.CompanyRegisterRequestDto;
import com.petmate.domain.company.dto.response.CompanyResponseDto;
import com.petmate.domain.company.service.CompanyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

            log.info("업체 등록 요청 - userId: {}, companyType: {}", userId, requestDto.getType());

            CompanyResponseDto company = companyService.registerCompany(requestDto, Integer.parseInt(userId));
            return ResponseEntity.ok(company);  // 직접 리턴
        }

    }






}
