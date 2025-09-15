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

import java.lang.reflect.Method;
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

    @Value("${app.nts.service-key:default-key}")
    private String ntsServiceKey;

    @Transactional
    public CompanyResponseDto registerCompany(CompanyRegisterRequestDto dto, Integer userId) {
        log.info("=== 업체 등록 시작 ===");
        log.info("userId: {}", userId);
        log.info("dto.getType(): {}", dto.getType());
        log.info("dto.getRepService(): {}", dto.getRepService());
        log.info("dto.getServices(): {}", dto.getServices());
        log.info("dto.getRoadAddr(): {}", dto.getRoadAddr());

        if (dto.getBizRegNo() != null && companyRepository.existsByBizRegNo(dto.getBizRegNo())) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }

        String type = "PERSONAL".equals(dto.getType()) ? "P" : "B";
        String repServiceCode = findServiceCodeByName(dto.getRepService());

        if (!codeUtil.isValidCode("COMPANY_TYPE", type)) {
            throw new IllegalArgumentException("유효하지 않은 업체 타입: " + type);
        }
        if (!codeUtil.isValidCode("SERVICE_TYPE", repServiceCode)) {
            throw new IllegalArgumentException("유효하지 않은 서비스 타입: " + repServiceCode);
        }

        // 호환 게터로 상호명 결정
        String companyName = coalesce(
                getByGetter(dto, "getName"),
                getByGetter(dto, "getCompanyName"),
                getByGetter(dto, "getPersonalCompanyName"),
                getByGetter(dto, "getCorporationName"),
                getByGetter(dto, "getCorpName"),
                getByGetter(dto, "getBusinessName")
        );
        if (companyName == null) throw new IllegalArgumentException("상호명은 필수입니다.");

        // 호환 게터로 대표자명 결정
        String repName = "PERSONAL".equals(dto.getType())
                ? coalesce(
                getByGetter(dto, "getPersonalName"),
                getByGetter(dto, "getOwnerName"),
                getByGetter(dto, "getRepresentativeName"),
                getByGetter(dto, "getRepName"))
                : coalesce(
                getByGetter(dto, "getRepName"),
                getByGetter(dto, "getRepresentativeName"),
                getByGetter(dto, "getCeoName"),
                getByGetter(dto, "getOwnerName"));
        if (repName == null) throw new IllegalArgumentException("대표자명은 필수입니다.");

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
                .status("P");

        if ("PERSONAL".equals(dto.getType())) {
            String encryptedSsnSecond = dto.getSsnSecond() != null
                    ? encryptionUtil.encrypt(dto.getSsnSecond()) : null;

            builder.ssnFirst(dto.getSsnFirst())
                    .ssnSecond(encryptedSsnSecond)
                    .personalName(coalesce(
                            getByGetter(dto, "getPersonalName"),
                            getByGetter(dto, "getOwnerName")));
        } else {
            builder.bizRegNo(dto.getBizRegNo());
        }

        CompanyEntity company = builder.build();
        CompanyEntity savedCompany = companyRepository.save(company);

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            log.info("업체 이미지 저장 시작 - 파일 개수: {}", dto.getImages().size());
            try {
                Long companyId = savedCompany.getId() != null ? savedCompany.getId().longValue() : null;
                if (companyId != null) {
                    List<com.petmate.common.entity.ImageEntity> savedImages = imageService.uploadMultipleImages(
                            dto.getImages(),
                            "03",
                            companyId,
                            true
                    );
                    log.info("업체 이미지 {} 개 저장 완료! 저장된 이미지 IDs: {}",
                            savedImages.size(),
                            savedImages.stream().map(img -> img.getId()).toList());
                }
            } catch (Exception e) {
                log.error("업체 이미지 저장 중 오류 발생: {}", e.getMessage(), e);
            }
        } else {
            log.info("업로드할 이미지가 없습니다.");
        }

        return mapToResponseDto(savedCompany);
    }

    public BusinessData checkBusinessNumber(String businessNumber) {
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

        String statusCode = switch (status) {
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

    private CompanyResponseDto mapToResponseDto(CompanyEntity entity) {
        return CompanyResponseDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .name(entity.getName())
                .bizRegNo(entity.getBizRegNo())
                .repName(entity.getRepName())
                .ssnFirst(entity.getSsnFirst())
                .ssnSecond(entity.getSsnSecond() != null ?
                        encryptionUtil.decrypt(entity.getSsnSecond()) : null)
                .personalName(entity.getPersonalName())
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

    private String findServiceCodeByName(String serviceName) {
        return switch (serviceName) {
            case "돌봄" -> "1";
            case "산책" -> "2";
            case "미용" -> "3";
            case "병원" -> "4";
            case "기타" -> "9";
            default -> throw new IllegalArgumentException("유효하지 않은 서비스 타입: " + serviceName);
        };
    }

    private BigDecimal parseBigDecimal(String value) {
        try {
            return value != null && !value.trim().isEmpty() ? new BigDecimal(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 게터 호환 유틸
    private String getByGetter(Object target, String getterName) {
        try {
            Method m = target.getClass().getMethod(getterName);
            Object v = m.invoke(target);
            if (v == null) return null;
            String s = String.valueOf(v).trim();
            return s.isEmpty() ? null : s;
        } catch (Exception ignore) {
            return null;
        }
    }

    // CompanyService.java 내부에 추가
    public BusinessValidationResult validateBusinessNumber(String businessNumber) {
        log.info("사업자등록번호 검증 시작: {}", businessNumber);

        try {
            String url = "http://api.odcloud.kr/api/nts-businessman/v1/validate"
                    + "?serviceKey=" + ntsServiceKey + "&returnType=JSON";

            Map<String, Object> requestBody = Map.of("b_no", List.of(businessNumber));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            Map<String, Object> body = response.getBody();
            log.info("국세청 API 응답: {}", body);

            if (body != null && "OK".equals(body.get("status_code"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) body.get("data");
                if (dataList != null && !dataList.isEmpty()) {
                    Map<String, Object> data = dataList.get(0);
                    String valid = (String) data.get("valid"); // "01" 유효, "02" 오류 등
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


    @SafeVarargs
    private final <T> T coalesce(T... vals) {
        for (T v : vals) if (v != null) return v;
        return null;
    }
}
