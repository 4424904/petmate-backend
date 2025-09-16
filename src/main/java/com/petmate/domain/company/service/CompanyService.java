package com.petmate.domain.company.service;

import com.petmate.common.service.ImageService;
import com.petmate.common.util.CodeUtil;
import com.petmate.domain.company.dto.request.CompanyRegisterRequestDto;
import com.petmate.domain.company.dto.request.CompanyUpdateRequestDto;
import com.petmate.domain.company.dto.response.BusinessInfoResponseDto;
import com.petmate.domain.company.dto.response.CompanyResponseDto;
import com.petmate.domain.company.entity.CompanyEntity;
import com.petmate.domain.company.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public CompanyResponseDto registerCompany(CompanyRegisterRequestDto dto, Integer userId) {
        log.info("=== 업체 등록 시작 ===");
        log.info("userId: {}", userId);
        log.info("dto.getType(): {}", dto.getType());
        log.info("dto.getRepresentativeName(): {}", dto.getRepresentativeName());
        log.info("dto.getCorporationName(): {}", dto.getCorporationName());
        log.info("dto.getRepService(): {}", dto.getRepService());
        log.info("dto.getServices(): {}", dto.getServices());
        log.info("dto.getRoadAddr(): {}", dto.getRoadAddr());

        if (dto.getBizRegNo() != null && companyRepository.existsByBizRegNo(dto.getBizRegNo())) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }

        String type = "PERSONAL".equals(dto.getType()) ? "P" : "B";

        String companyName = "PERSONAL".equals(dto.getType())
                ? dto.getRepresentativeName()
                : dto.getCorporationName();

        if ("P".equals(type) && companyName != null) {
            boolean existsPersonalCompany = companyRepository.existsByTypeAndNameAndCreatedBy(
                    "P", companyName, userId
            );
            if (existsPersonalCompany) {
                throw new IllegalArgumentException("이미 해당 이름으로 등록된 개인 업체가 있습니다.");
            }
        }
        String repServiceCode = findServiceCodeByName(dto.getRepService());

        log.info("검증할 서비스 타입 코드: {}", repServiceCode);
        log.info("SERVICE_TYPE 그룹의 모든 코드: {}", codeUtil.getCodeMap("SERVICE_TYPE"));

        boolean isValidServiceType = codeUtil.isValidCode("SERVICE_TYPE", repServiceCode);
        log.info("SERVICE_TYPE {} 유효성 검증 결과: {}", repServiceCode, isValidServiceType);

        if (!isValidServiceType) {
            throw new IllegalArgumentException("유효하지 않은 서비스 타입: " + repServiceCode);
        }

        String repName = "PERSONAL".equals(dto.getType())
                ? dto.getRepresentativeName()
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
                .latitude(parseBigDecimal(dto.getLatitude()))
                .longitude(parseBigDecimal(dto.getLongitude()))
                .createdBy(userId)
                .descText(dto.getIntroduction())
                .createdAt(java.time.LocalDateTime.now())
                .status("P");

        if ("PERSONAL".equals(dto.getType())) {
            log.info("개인 업체 등록: repName={}", dto.getRepresentativeName());
            builder.ssnFirst(dto.getSsnFirst());
        } else {
            builder.bizRegNo(dto.getBizRegNo());
        }

        CompanyEntity company = builder.build();
        CompanyEntity savedCompany = companyRepository.save(company);

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            log.info("업체 이미지 저장 시작 - 파일 개수: {}", dto.getImages().size());
            try {
                imageService.uploadMultipleImages(
                        dto.getImages(),
                        "03",
                        savedCompany.getId().longValue(),
                        true
                );
            } catch (Exception e) {
                log.error("업체 이미지 저장 중 오류 발생: {}", e.getMessage(), e);
            }
        } else {
            log.info("업로드할 이미지가 없습니다.");
        }

        return mapToResponseDto(savedCompany);
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
        if (dto.getLatitude() != null) company.setLatitude(parseBigDecimal(dto.getLatitude()));
        if (dto.getLongitude() != null) company.setLongitude(parseBigDecimal(dto.getLongitude()));
        if (dto.getServices() != null) company.setServices(dto.getServices());
        if (dto.getOperatingHours() != null) company.setOperatingHours(dto.getOperatingHours());
        if (dto.getRepService() != null) {
            String repServiceCode = findServiceCodeByName(dto.getRepService());
            company.setRepService(repServiceCode);
        }

        company.setUpdatedAt(java.time.LocalDateTime.now());

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

    /** 인근 업체 조회 */
    public List<CompanyResponseDto> getNearbyCompanies(Double lat, Double lng, Double radiusKm, String serviceType) {
        if (lat == null || lng == null) return List.of();
        double r = (radiusKm == null || radiusKm <= 0) ? 5.0 : radiusKm;

        // 저장소에 전용 쿼리가 없다면 전체에서 필터링
        return companyRepository.findAll().stream()
                .filter(c -> c.getLatitude() != null && c.getLongitude() != null)
                .filter(c -> {
                    if (serviceType == null || serviceType.isBlank()) return true;
                    String rep = c.getRepService();
                    String services = c.getServices();
                    return serviceType.equals(rep) ||
                            (services != null && services.contains(serviceType));
                })
                .filter(c -> haversineKm(
                        lat, lng,
                        c.getLatitude().doubleValue(),
                        c.getLongitude().doubleValue()
                ) <= r)
                .map(this::mapToResponseDto)
                .toList();
    }

    /** Haversine 거리(km) */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0088;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /** 프론트엔드용 공통코드 */
    public Map<String, String> getCompanyTypeMap() { return codeUtil.getCodeMap("COMPANY_TYPE"); }
    public Map<String, String> getServiceTypeMap() { return codeUtil.getCodeMap("SERVICE_TYPE"); }
    public Map<String, String> getCompanyStatusMap() { return codeUtil.getCodeMap("COMPANY_STATUS"); }

    // ================================
    // Private
    // ================================
    private CompanyResponseDto mapToResponseDto(CompanyEntity entity) {
        return CompanyResponseDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .name(entity.getName())
                .bizRegNo(entity.getBizRegNo())
                .repName(entity.getRepName())
                .ssnFirst(entity.getSsnFirst())
                .tel(entity.getTel())
                .repService(entity.getRepService())
                .services(entity.getServices())
                .operatingHours(entity.getOperatingHours())
                .status(entity.getStatus())
                .roadAddr(entity.getRoadAddr())
                .detailAddr(entity.getDetailAddr())
                .postcode(entity.getPostcode())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .descText(entity.getDescText())
                .build();
    }

    private String findServiceCodeByName(String serviceName) {
        return switch (serviceName) {
            case "돌봄", "1" -> "1";
            case "산책", "2" -> "2";
            case "미용", "3" -> "3";
            case "병원", "4" -> "4";
            case "기타", "9" -> "9";
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

    public BusinessInfoResponseDto getBusinessInfo(String businessNumber) {
        log.info("사업자등록번호 중복 체크 시작: 사업자번호={}", businessNumber);
        try {
            boolean isDuplicate = companyRepository.existsByBizRegNo(businessNumber);
            if (isDuplicate) {
                return BusinessInfoResponseDto.builder()
                        .businessNumber(businessNumber)
                        .isValid(false)
                        .message("이미 등록된 사업자등록번호입니다")
                        .build();
            } else {
                return BusinessInfoResponseDto.builder()
                        .businessNumber(businessNumber)
                        .isValid(true)
                        .message("등록 가능한 사업자등록번호입니다")
                        .build();
            }
        } catch (Exception e) {
            log.error("사업자등록번호 중복 체크 중 오류 발생: ", e);
            return BusinessInfoResponseDto.builder()
                    .businessNumber(businessNumber)
                    .isValid(false)
                    .message("사업자등록번호 확인 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }
}
