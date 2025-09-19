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
        log.info("userId: {}", String.valueOf(userId));
        log.info("dto.getType(): {}", dto.getType());
        log.info("dto.getRepresentativeName(): {}", dto.getRepresentativeName());
        log.info("dto.getCorporationName(): {}", dto.getCorporationName());
        log.info("dto.getRepService(): {}", dto.getRepService());
        log.info("dto.getServices(): {}", dto.getServices());
        log.info("dto.getRoadAddr(): {}", dto.getRoadAddr());
        // 사업자번호 중복 체크
        if (dto.getBizRegNo() != null && companyRepository.existsByBizRegNo(dto.getBizRegNo())) {
            throw new IllegalArgumentException("이미 등록된 사업자등록번호입니다.");
        }

        // CodeUtil 활용한 변환
        String type = "PERSONAL".equals(dto.getType()) ? "P" : "B";

        // 상호명 결정 (개인 업체 중복 체크를 위해 미리 계산)
        String companyName = "PERSONAL".equals(dto.getType())
                ? dto.getRepresentativeName() // 개인: 대표자명 = 개인명
                : dto.getCorporationName();

        // 개인 업체 중복 체크 (한 사람당 하나의 개인 업체만 등록 가능)
        if ("P".equals(type) && companyName != null) {
            boolean existsPersonalCompany = companyRepository.existsByTypeAndNameAndCreatedBy(
                    "P", companyName, userId
            );
            if (existsPersonalCompany) {
                throw new IllegalArgumentException("이미 해당 이름으로 등록된 개인 업체가 있습니다.");
            }
        }
        String repServiceCode = findServiceCodeByName(dto.getRepService());

        // 유효성 검증 (디버깅 추가)
        log.info("검증할 서비스 타입 코드: {}", repServiceCode);
        log.info("SERVICE_TYPE 그룹의 모든 코드: {}", codeUtil.getCodeMap("SERVICE_TYPE"));

        boolean isValidServiceType = codeUtil.isValidCode("SERVICE_TYPE", repServiceCode);
        log.info("SERVICE_TYPE {} 유효성 검증 결과: {}", repServiceCode, isValidServiceType);

        if (!isValidServiceType) {
            throw new IllegalArgumentException("유효하지 않은 서비스 타입: " + repServiceCode);
        }

        // 대표자명 결정
        String repName = "PERSONAL".equals(dto.getType())
                ? dto.getRepresentativeName() // 개인: 대표자명 = 개인명
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
                .status("P");  // 명시적으로 승인대기 상태 설정

        // 개인(일반인) vs 사업자별 추가 정보
        if ("PERSONAL".equals(dto.getType())) {
            // JWT 토큰 기반 신원 인증을 별도 API로 처리 완료
            log.info("개인 업체 등록: repName={}", dto.getRepresentativeName());

            builder.ssnFirst(dto.getSsnFirst());
            builder.bizRegNo(dto.getSsnFirst());
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
                        savedCompany.getId().toString(),   // 업체 ID (Long 타입으로 변환)
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

    /**
     * 업체 ID만으로 업체 정보 조회 (타임슬롯 조회용)
     * 권한 체크 없이 승인된 업체만 조회
     */
    public CompanyResponseDto getCompanyByIdPublic(Integer id) {
        CompanyEntity company = companyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("업체를 찾을 수 없습니다."));

        // 승인된 업체만 허용 (보안)
        if (!"A".equals(company.getStatus())) {
            throw new IllegalArgumentException("승인되지 않은 업체입니다.");
        }

        return mapToResponseDto(company);
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
        if (dto.getLatitude() != null) company.setLatitude(parseBigDecimal(dto.getLatitude()));
        if (dto.getLongitude() != null) company.setLongitude(parseBigDecimal(dto.getLongitude()));
        if (dto.getServices() != null) company.setServices(dto.getServices());
        if (dto.getOperatingHours() != null) company.setOperatingHours(dto.getOperatingHours());
        if (dto.getRepService() != null) {
            String repServiceCode = findServiceCodeByName(dto.getRepService());
            company.setRepService(repServiceCode);
        }

        // 수정일 자동 설정
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
                .ssnFirst(entity.getSsnFirst())
                // 기존 필드들
                .tel(entity.getTel())
                .repService(entity.getRepService())
                .services(entity.getServices())
                .operatingHours(entity.getOperatingHours())
                .status(entity.getStatus())
                .roadAddr(entity.getRoadAddr())
                .detailAddr(entity.getDetailAddr())
                .postcode(entity.getPostcode())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude ())
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
        // 한글 서비스명 또는 코드를 코드로 변환
        return switch(serviceName) {
            case "돌봄", "1" -> "1";
            case "산책", "2" -> "2";
            case "미용", "3" -> "3";
            case "병원", "4" -> "4";
            case "기타", "9" -> "9";
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
     * 사업자등록번호 중복 체크 (DB 기반)
     */
    public BusinessInfoResponseDto getBusinessInfo(String businessNumber) {
        log.info("사업자등록번호 중복 체크 시작: 사업자번호={}", businessNumber);

        try {
            // DB에서 해당 사업자등록번호가 이미 등록되어 있는지 확인
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

    public List<CompanyResponseDto> getNearbyCompanies(
            Double userLat,
            Double userLng,
            Double radiusKm,
            String serviceType
    ) {

        // 승인된 업체만 조회
        List<CompanyEntity> approvedCompanies = companyRepository.findByStatusOrderByCreatedAtDesc("A");
        log.info("승인된 업체 총 {}개", approvedCompanies.size());

        return approvedCompanies.stream()
                .filter(company -> company.getLatitude() != null && company.getLongitude() != null) // 좌표 있는 업체만 조회
                .filter(company -> {
                    // 하버사인 거리 계산
                    double distance = calculateDistance(
                            userLat,
                            userLng,
                            company.getLatitude().doubleValue(),
                            company.getLongitude().doubleValue()
                    );

                    log.info("업체 '{}': 좌표({}, {}), 거리={}km, 반경내={}",
                            company.getName(),
                            company.getLatitude(),
                            company.getLongitude(),
                            distance,
                            distance <= radiusKm);

                    return distance <= radiusKm;
                })
                .filter(company -> serviceType == null || serviceType.isEmpty() || company.getRepService().equals(serviceType))
                .map(this::mapToResponseDto)
                .toList();

    }


    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {

        final int R = 6371; // 지구 반지름

        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return R*c; // 거리(km)

    }

}