package com.petmate.domain.company.service;

import com.petmate.common.service.ImageService;
import com.petmate.common.util.CodeUtil;
import com.petmate.common.entity.ImageEntity;
import com.petmate.common.repository.ImageRepository;
import com.petmate.domain.company.dto.request.CompanyRegisterRequestDto;
import com.petmate.domain.company.dto.request.CompanyUpdateRequestDto;
import com.petmate.domain.company.dto.response.BusinessInfoResponseDto;
import com.petmate.domain.company.dto.response.CompanyResponseDto;
import com.petmate.domain.company.dto.response.CompanyImageDto;
import com.petmate.domain.company.entity.CompanyEntity;
import com.petmate.domain.company.repository.CompanyRepository;
import com.petmate.domain.company.util.BusinessHoursCalculator;
import com.petmate.domain.company.util.ServiceParser;
import com.petmate.common.util.DistanceCalculatorUtil;
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
    private final ImageRepository imageRepository;

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

        // 개인 업체 중복 체크 (한 사용자당 하나의 개인 업체만 등록 가능)
        if ("P".equals(type)) {
            boolean existsPersonalCompany = companyRepository.existsByCreatedByAndType(userId, "P");
            if (existsPersonalCompany) {
                throw new IllegalArgumentException("이미 등록된 개인 업체가 있습니다. 한 사용자는 하나의 개인 업체만 등록할 수 있습니다.");
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
                .status("A");  // 명시적으로 승인대기 상태 설정

        // 개인(일반인) vs 사업자별 추가 정보
        if ("PERSONAL".equals(dto.getType())) {
            // JWT 토큰 기반 신원 인증을 별도 API로 처리 완료
            log.info("개인 업체 등록: repName={}", dto.getRepresentativeName());

            // 개인업체 biz_reg_no 자동 생성 (생년월일 + 순차번호)
            String generatedBizRegNo = generatePersonalBizRegNo(dto.getSsnFirst());

            builder.ssnFirst(dto.getSsnFirst());
            builder.bizRegNo(generatedBizRegNo);
        } else {
            builder.bizRegNo(dto.getBizRegNo());
        }

        CompanyEntity company = builder.build();

        CompanyEntity savedCompany = companyRepository.save(company);

        // 업체 이미지 저장 (IMAGE_TYPE: 03 - COMPANY_REG)
        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            log.info("업체 이미지 저장 시작 - 파일 개수: {}", dto.getImages().size());
            try {
                // 이미지 저장용 reference_id 생성 (개인업체는 하이픈 제거한 생년월일만 사용)
                String imageReferenceId = "P".equals(savedCompany.getType()) ?
                        savedCompany.getSsnFirst() : savedCompany.getBizRegNo();

                log.info("이미지 저장 - reference_id: {}, type: {}", imageReferenceId, savedCompany.getType());

                List<com.petmate.common.entity.ImageEntity> savedImages = imageService.uploadMultipleImages(
                        dto.getImages(),        // 업로드할 파일들
                        "03",                   // IMAGE_TYPE 코드 (COMPANY_REG)
                        imageReferenceId,       // 개인: 생년월일(하이픈X), 사업자: 사업자번호
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

        // 계산 로직 실행
        Map<String, String> businessStatus = BusinessHoursCalculator.calculateCurrentBusinessStatus(entity.getOperatingHours());
        String todayHours = BusinessHoursCalculator.calculateTodayHours(entity.getOperatingHours());
        List<String> serviceNames = ServiceParser.parseServices(entity.getServices(), entity.getRepService());
        List<Map<String, String>> weeklySchedule = BusinessHoursCalculator.calculateWeeklySchedule(entity.getOperatingHours());

        // 업체 이미지 조회 (개인업체는 하이픈 제거한 생년월일로 조회)
        String imageReferenceId = "P".equals(entity.getType()) ?
                entity.getSsnFirst() : entity.getBizRegNo();
        List<CompanyImageDto> images = getCompanyImages(imageReferenceId);


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
                // 계산된 필드 추가
                .currentBusinessStatus(businessStatus.get("status"))
                .currentBusinessMessage(businessStatus.get("message"))
                .todayHours(todayHours)
                .serviceNames(serviceNames)
                .weeklySchedule(weeklySchedule)
                .images(images)
                .build();
    }

    /**
     * 업체 이미지 조회 (reference_id = biz_no, reference_type = "03")
     */
    private List<CompanyImageDto> getCompanyImages(String bizRegNo) {
        log.info("업체 이미지 조회 시작 - bizRegNo: {}", bizRegNo);

        if (bizRegNo == null || bizRegNo.trim().isEmpty()) {
            log.warn("bizRegNo가 null 또는 빈 문자열입니다.");
            return List.of(); // 빈 리스트 반환
        }

        try {
            List<ImageEntity> imageEntities = imageRepository.findActiveImagesByReference("03", bizRegNo);
            log.info("조회된 이미지 개수: {} - bizRegNo: {}", imageEntities.size(), bizRegNo);

            return imageEntities.stream()
                    .map(image -> {
                        log.info("이미지 변환 중 - id: {}, filePath: {}, displayOrder: {}",
                                image.getId(), image.getFilePath(), image.getDisplayOrder());
                        return CompanyImageDto.builder()
                                .id(image.getId())
                                .filePath(image.getFilePath())
                                .originalName(image.getOriginalName())
                                .altText(image.getAltText())
                                .description(image.getDescription())
                                .displayOrder(image.getDisplayOrder())
                                .isThumbnail("Y".equals(image.getIsThumbnail()))
                                .mimeType(image.getMimeType())
                                .fileSize(image.getFileSize())
                                .build();
                    })
                    .toList();
        } catch (Exception e) {
            log.error("업체 이미지 조회 중 오류 발생 - bizRegNo: {}, error: {}", bizRegNo, e.getMessage(), e);
            return List.of(); // 오류 시 빈 리스트 반환
        }
    }

    /**
     * 개인업체 biz_reg_no 자동 생성 (생년월일 + 순차번호)
     */
    private String generatePersonalBizRegNo(String ssnFirst) {
        log.info("개인업체 biz_reg_no 생성 시작 - ssnFirst: {}", ssnFirst);

        // 해당 생년월일로 시작하는 biz_reg_no 개수 조회
        long count = companyRepository.countBySsnFirstPattern(ssnFirst);

        // 순차번호 생성 (1부터 시작, 5자리 패딩)
        long nextNumber = count + 1;
        String sequenceNumber = String.format("%05d", nextNumber);

        String generatedBizRegNo = ssnFirst + "-" + sequenceNumber;

        log.info("개인업체 biz_reg_no 생성 완료 - {} (기존 개수: {})", generatedBizRegNo, count);

        return generatedBizRegNo;
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
                .map(company -> {
                    // 거리 계산
                    double distance = DistanceCalculatorUtil.calculateDistance(
                            userLat,
                            userLng,
                            company.getLatitude().doubleValue(),
                            company.getLongitude().doubleValue()
                    );

                    // dto 생성 후 거리 설정
                    CompanyResponseDto companyResponseDto = mapToResponseDto(company);
                    companyResponseDto.setDistanceKm(distance);

                    log.info("업체 '{}': 좌표({},{}), 거리={}km",
                            company.getName(),
                            company.getLatitude(),
                            company.getLongitude(),
                            distance);

                    return companyResponseDto;
                })
                .filter(dto -> dto.getDistanceKm() <= radiusKm)
                .filter(dto -> serviceType == null || serviceType.isEmpty() ||
                        dto.getRepService().equals(serviceType))
                .sorted((a, b) -> Double.compare(a.getDistanceKm(), b.getDistanceKm())) // 거리순 정렬
                .toList();
    }

    // 개인 업체 등록 여부 확인 (createdBy 기반)
    public boolean checkPersonalCompanyExists(Integer userId) {
        log.info("개인 업체 중복 확인 - userId: {}", userId);

        boolean exists = companyRepository.existsByCreatedByAndType(userId, "P");

        log.info("개인 업체 중복 확인 결과 - userId: {}, exists: {}", userId, exists);

        return exists;
    }

}