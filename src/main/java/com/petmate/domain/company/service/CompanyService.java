package com.petmate.domain.company.service;

import com.petmate.common.service.ImageService;
import com.petmate.common.util.CodeUtil;
import com.petmate.util.EncryptionUtil;
import com.petmate.domain.company.dto.request.CompanyRegisterRequestDto;
import com.petmate.domain.company.dto.request.CompanyUpdateRequestDto;
import com.petmate.domain.company.dto.response.BusinessData;
import com.petmate.domain.company.dto.response.BusinessValidationResult;
import com.petmate.domain.company.dto.response.CompanyResponseDto;
import com.petmate.domain.company.entity.CompanyEntity;
import com.petmate.domain.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CodeUtil codeUtil;
    private final ImageService imageService;
    private final EncryptionUtil encryptionUtil;

    @Value("${app.nts.service-key}")
    private String ntsServiceKey;

    @Transactional
    public CompanyResponseDto registerCompany(CompanyRegisterRequestDto dto, Integer userId) {
        log.info("=== 업체 등록 시작 ===");
        log.info("userId: {}", userId);
        log.info("dto.getType(): {}", dto.getType());
        log.info("dto.getRepService(): {}", dto.getRepService());
        log.info("dto.getServices(): {}", dto.getServices());
        log.info("dto.getRoadAddr(): {}", dto.getRoadAddr());
        // 사업자번호 중복 체크
        if (dto.getBizRegNo() != null && companyRepository.existsByBizRegNo(dto.getBizRegNo())) {
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

        CompanyEntity.CompanyEntityBuilder builder = CompanyEntity.builder()
                .type(type)
                .name(companyName)
                .repName(repName)
                .tel(dto.getTel())
                .repService(repServiceCode)
                .services(dto.getServices())
                .operatingHours(dto.getOperatingHours())
                .roadAddr(dto.getRoadAddr())
                .detailAddr(dto.getDetailAddr())
                .postcode(dto.getPostcode())
                .lat(parseBigDecimal(dto.getLatitude()))
                .lng(parseBigDecimal(dto.getLongitude()))
                .createdBy(userId)
                .descText(dto.getIntroduction())
                .createdAt(java.time.LocalDateTime.now())
                .status("P");  // 명시적으로 승인대기 상태 설정

        // 개인(일반인) vs 사업자별 추가 정보
        if ("PERSONAL".equals(dto.getType())) {
            // 주민번호 뒷자리 암호화
            String encryptedSsnSecond = dto.getSsnSecond() != null ? 
                encryptionUtil.encrypt(dto.getSsnSecond()) : null;
            
            builder.ssnFirst(dto.getSsnFirst())
                   .ssnSecond(encryptedSsnSecond)
                   .personalName(dto.getPersonalName());
        } else {
            builder.bizRegNo(dto.getBizRegNo());
        }

        CompanyEntity company = builder.build();

        CompanyEntity savedCompany = companyRepository.save(company);

        // 업체 이미지 저장 (IMAGE_TYPE: 03 - COMPANY_REG)
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            log.info("업체 이미지 저장 시작 - 파일 개수: {}", dto.getImages().size());
            try {
                List<com.petmate.common.entity.ImageEntity> savedImages = imageService.uploadMultipleImages(
                    dto.getImages(),        // 업로드할 파일들
                    "03",                   // IMAGE_TYPE 코드 (COMPANY_REG)
                    savedCompany.getId().longValue(),   // 업체 ID (Long 타입으로 변환)
                    true                    // 첫 번째 이미지를 썸네일로 설정
                );
                log.info("업체 이미지 {} 개 저장 완료! 저장된 이미지 IDs: {}",
                    savedImages.size(),
                    savedImages.stream().map(img -> img.getId()).toList());
            } catch (Exception e) {
                log.error("업체 이미지 저장 중 오류 발생: {}", e.getMessage(), e);
                // 이미지 저장 실패해도 업체 등록은 완료되도록 처리
            }
        } else {
            log.info("업로드할 이미지가 없습니다.");
        }

        return mapToResponseDto(savedCompany);
    }

    public BusinessData checkBusinessNumber(String businessNumber) {
        // 실제로는 국세청 API 호출
        return BusinessData.builder()
                .companyName("펫메이트(주)")
                .representativeName("홍길동")
                .build();
    }

    public CompanyResponseDto getCompanyById(Integer id, Integer userId) {
        CompanyEntity company = companyRepository.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        return mapToResponseDto(company);
    }

    @Transactional
    public CompanyResponseDto updateCompany(Integer id, CompanyUpdateRequestDto dto, Integer userId) {
        CompanyEntity company = companyRepository.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));

        // 업데이트 로직
        if (dto.getName() != null) company.setName(dto.getName());
        if (dto.getTel() != null) company.setTel(dto.getTel());
        if (dto.getDescText() != null) company.setDescText(dto.getDescText());
        if (dto.getRoadAddr() != null) company.setRoadAddr(dto.getRoadAddr());
        if (dto.getDetailAddr() != null) company.setDetailAddr(dto.getDetailAddr());
        if (dto.getPostcode() != null) company.setPostcode(dto.getPostcode());
        if (dto.getLatitude() != null) company.setLat(parseBigDecimal(dto.getLatitude()));
        if (dto.getLongitude() != null) company.setLng(parseBigDecimal(dto.getLongitude()));
        if (dto.getServices() != null) company.setServices(dto.getServices());
        if (dto.getOperatingHours() != null) company.setOperatingHours(dto.getOperatingHours());
        if (dto.getRepService() != null) {
            String repServiceCode = findServiceCodeByName(dto.getRepService());
            company.setRepService(repServiceCode);
        }

        CompanyEntity savedCompany = companyRepository.save(company);
        return mapToResponseDto(savedCompany);
    }

    @Transactional
    public void deleteCompany(Integer id, Integer userId) {
        CompanyEntity company = companyRepository.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));
        companyRepository.delete(company);
    }

    @Transactional
    public CompanyResponseDto updateCompanyStatus(Integer id, String status, Integer userId) {
        CompanyEntity company = companyRepository.findByIdAndCreatedBy(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));

        // 상태 코드 변환 (필요시)
        String statusCode = switch(status) {
            case "PENDING", "P" -> "P";
            case "APPROVED", "A" -> "A";
            case "REJECTED", "R" -> "R";
            default -> throw new IllegalArgumentException("유효하지 않은 상태: " + status);
        };

        company.setStatus(statusCode);
        CompanyEntity savedCompany = companyRepository.save(company);
        return mapToResponseDto(savedCompany);
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
                // 개인(일반인) 정보 추가 (주민번호 뒷자리 복호화)
                .ssnFirst(entity.getSsnFirst())
                .ssnSecond(entity.getSsnSecond() != null ? 
                    encryptionUtil.decrypt(entity.getSsnSecond()) : null)
                .personalName(entity.getPersonalName())
                // 기존 필드들
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
     * 한글 서비스명을 코드로 변환
     */
    private String findServiceCodeByName(String serviceName) {
        // 한글 서비스명을 코드로 직접 매핑
        return switch(serviceName) {
            case "돌봄" -> "1";
            case "산책" -> "2";
            case "미용" -> "3";
            case "병원" -> "4";
            case "기타" -> "9";
            default -> throw new IllegalArgumentException("유효하지 않은 서비스 타입: " + serviceName);
        };
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

    /**
     * 국세청 API를 통한 사업자등록번호 검증
     */
    public BusinessValidationResult validateBusinessNumber(String businessNumber) {
        log.info("사업자등록번호 검증 시작: {}", businessNumber);
        
        try {
            // API URL 구성
            String url = "http://api.odcloud.kr/api/nts-businessman/v1/validate?serviceKey=" 
                        + ntsServiceKey + "&returnType=JSON";
            
            // 요청 데이터 생성
            Map<String, Object> requestBody = Map.of("b_no", List.of(businessNumber));
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // HTTP 요청 엔티티 생성
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            // RestTemplate으로 API 호출
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            
            // 응답 처리
            Map<String, Object> responseBody = response.getBody();
            log.info("국세청 API 응답: {}", responseBody);
            
            if (responseBody != null && "OK".equals(responseBody.get("status_code"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) responseBody.get("data");
                
                if (!dataList.isEmpty()) {
                    Map<String, Object> data = dataList.get(0);
                    String valid = (String) data.get("valid");
                    
                    boolean isValid = "01".equals(valid);
                    String message = isValid ? "유효한 사업자등록번호입니다" : "무효한 사업자등록번호입니다";
                    
                    return BusinessValidationResult.builder()
                        .businessNumber(businessNumber)
                        .isValid(isValid)
                        .status(valid)
                        .message(message)
                        .build();
                }
            }
            
            // 응답이 없거나 오류인 경우
            return BusinessValidationResult.builder()
                .businessNumber(businessNumber)
                .isValid(false)
                .message("사업자등록번호를 조회할 수 없습니다")
                .build();
                
        } catch (Exception e) {
            log.error("사업자등록번호 검증 중 오류 발생: ", e);
            return BusinessValidationResult.builder()
                .businessNumber(businessNumber)
                .isValid(false)
                .message("사업자등록번호 검증 중 오류가 발생했습니다: " + e.getMessage())
                .build();
        }
    }
}