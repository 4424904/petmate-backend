package com.petmate.domain.company.service;

import com.petmate.common.util.CodeUtil;
import com.petmate.domain.company.dto.request.CompanyRegisterRequestDto;
import com.petmate.domain.company.dto.response.BusinessData;
import com.petmate.domain.company.dto.response.CompanyResponseDto;
import com.petmate.domain.company.entity.CompanyEntity;
import com.petmate.domain.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CodeUtil codeUtil;

    @Transactional
    public CompanyResponseDto registerCompany(CompanyRegisterRequestDto dto, Integer userId) {
        // 사업자번호 중복 체크
        if (dto.getBizRegNo() != null && companyRepository.existByBizRegNo(dto.getBizRegNo())) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }

        // CodeUtil 활용한 변환
        String type = "PERSONAL".equals(dto.getType()) ? "P" : "B";
        String repServiceCode = findServiceCodeByName(dto.getRepService());

        // 유효성 검증
        if (!codeUtil.isValidCode("COMPANY_TYPE", type)) {
            throw new IllegalArgumentException("유효하지 않은 업체 타입: " + type);
        }
        if (!codeUtil.isValidCode("SERVICE_TYPE", repServiceCode)) {
            throw new IllegalArgumentException("유효하지 않은 서비스 타입: " + repServiceCode);
        }

        // 상호명 결정
        String companyName = "PERSONAL".equals(dto.getType())
                ? dto.getPersonalCompanyName()
                : dto.getCorporationName();

        // 대표자명 결정
        String repName = "PERSONAL".equals(dto.getType())
                ? dto.getPersonalName()
                : dto.getRepresentativeName();

        CompanyEntity company = CompanyEntity.builder()
                .type(type)
                .name(companyName)
                .bizRegNo(dto.getBizRegNo())
                .repName(repName)
                .tel(dto.getTel())
                .repService(repServiceCode)
                .services(dto.getServices())
                .operatingHours(dto.getOperatingHours())
                .roadAddr(dto.getRoadAddress())
                .detailAddr(dto.getDetailAddress())
                .postcode(dto.getPostcode())
                .lat(parseBigDecimal(dto.getLatitude()))
                .lng(parseBigDecimal(dto.getLongitude()))
                .createdBy(userId)
                .descText(dto.getIntroduction())
                .build();

        CompanyEntity savedCompany = companyRepository.save(company);
        return mapToResponseDto(savedCompany);
    }

    public BusinessCheckResponseDto checkBusinessNumber(String businessNumber) {
        // 실제로는 국세청 API 호출
        BusinessData data = BusinessData.builder()
                .companyName("펫메이트(주)")
                .representativeName("홍길동")
                .build();

        return BusinessCheckResponseDto.builder()
                .success(true)
                .message("조회 성공")
                .data(data)
                .build();
    }

    public List<CompanyResponseDto> getMyCompanies(Integer userId) {
        return companyRepository.findByCreatedByOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponseDto)
                .toList();
    }

    /**
     * 프론트엔드용 공통코드 목록 조회
     */
    public Map<String, String> getCompanyTypeMap() {
        return codeUtil.getCodeMap("COMPANY_TYPE");
    }

    public Map<String, String> getServiceTypeMap() {
        return codeUtil.getCodeMap("SERVICE_TYPE");
    }

    public Map<String, String> getCompanyStatusMap() {
        return codeUtil.getCodeMap("COMPANY_STATUS");
    }

    // ================================
    // Private 메서드들
    // ================================

    /**
     * Entity → ResponseDto 매핑 (공통코드명 포함)
     */
    private CompanyResponseDto mapToResponseDto(CompanyEntity entity) {
        return CompanyResponseDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .name(entity.getName())
                .bizRegNo(entity.getBizRegNo())
                .repName(entity.getRepName())
                .tel(entity.getTel())
                .repService(entity.getRepService())
                .services(entity.getServices())
                .operatingHours(entity.getOperatingHours())
                .status(entity.getStatus())
                .roadAddr(entity.getRoadAddr())
                .detailAddr(entity.getDetailAddr())
                .postcode(entity.getPostcode())
                .lat(entity.getLat())
                .lng(entity.getLng())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .descText(entity.getDescText())
                .build();
    }

    /**
     * 서비스명으로 서비스 타입 코드 찾기
     * CodeUtil의 getCodeMap()을 활용
     */
    private String findServiceCodeByName(String serviceName) {
        Map<String, String> serviceMap = codeUtil.getCodeMap("SERVICE_TYPE");

        return serviceMap.entrySet().stream()
                .filter(entry -> serviceName.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 서비스 타입: " + serviceName));
    }

    /**
     * 문자열을 BigDecimal로 안전하게 변환
     */
    private BigDecimal parseBigDecimal(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? new BigDecimal(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}