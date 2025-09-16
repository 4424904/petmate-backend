package com.petmate.domain.address.service;

import com.petmate.common.util.CodeUtil;
import com.petmate.domain.address.dto.request.AddressCreateRequestDto;
import com.petmate.domain.address.dto.request.AddressUpdateRequestDto;
import com.petmate.domain.address.dto.response.AddressResponseDto;
import com.petmate.domain.address.entity.AddressEntity;
import com.petmate.domain.address.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AddressService {

    private final AddressRepository addressRepository;
    private final CodeUtil codeUtil;

    // 프론트엔드 타입명을 ADDRESS_LABEL 코드로 변환
    private String convertTypeToLabelCode(String typeName) {
        try {
            String labelCode = switch (typeName) {
                case "집", "home" -> "1";
                case "회사", "work" -> "2";
                case "기타", "etc" -> "3";
                default -> throw new IllegalArgumentException("유효하지 않은 주소 타입: " + typeName);
            };

            // 공통코드 유효성 검증
            if (!codeUtil.isValidCode("ADDRESS_LABEL", labelCode)) {
                log.error("유효하지 않은 ADDRESS_LABEL 코드: {}", labelCode);
                throw new IllegalArgumentException("유효하지 않은 주소 라벨 코드: " + labelCode);
            }

            return labelCode;
        } catch (Exception e) {
            log.error("타입 변환 실패: {}, 오류: {}", typeName, e.getMessage());
            throw e;
        }
    }

    // 기본 한국어 버전
    private String convertLabelCodeToType(String labelCode) {
        return convertLabelCodeToType(labelCode, "ko");
    }

    // 다국어 지원 버전 (나중에 필요하면 사용)
    private String convertLabelCodeToType(String labelCode, String language) {
        try {
            String typeName;

            if ("en".equalsIgnoreCase(language)) {
                typeName = codeUtil.getCodeNameEng("ADDRESS_LABEL", labelCode);
            } else {
                typeName = codeUtil.getAddressLabelName(labelCode);
            }

            if (typeName == null || typeName.isEmpty()) {
                log.warn("ADDRESS_LABEL 코드에 대한 이름을 찾을 수 없음: {}", labelCode);
                return "en".equalsIgnoreCase(language) ? "etc" : "기타";
            }

            return typeName;
        } catch (Exception e) {
            log.error("라벨 변환 실패: {}, 언어: {}, 오류: {}", labelCode, language, e.getMessage());
            return "en".equalsIgnoreCase(language) ? "etc" : "기타";
        }
    }


    // jwt String userId를 integer ownerId로 변환
    private Integer convertUserIdToOwnerId(String jwtUserId) {
        if (jwtUserId == null || jwtUserId.trim().isEmpty()) {
            log.error("JWT userId가 null이거나 빈 값입니다");
            throw new IllegalArgumentException("사용자 ID가 없습니다");
        }

        try {
            Integer result = Integer.parseInt(jwtUserId.trim());
            log.debug("JWT userId 변환 성공: '{}' → {}", jwtUserId, result);
            return result;
        } catch (NumberFormatException e) {
            log.error("JWT userId를 Integer로 변환 실패: '{}'", jwtUserId);
            throw new IllegalArgumentException("유효하지 않은 사용자 아이디입니다: " + jwtUserId);
        }
    }

    // 사용자 주소 목록 조회
    @Transactional(readOnly = true)
    public List<AddressResponseDto> getUserAddresses(String jwtUserId) {
        log.info("사용자 주소 목록 조회 시작 - userId: {}", jwtUserId);

        try {
            Integer ownerId = convertUserIdToOwnerId(jwtUserId);

            List<AddressEntity> addresses = addressRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
            log.info("사용자 주소 목록 조회 완료 - ownerId: {}, 개수: {}", ownerId, addresses.size());

            return addresses.stream()
                    .map(this::convertToResponseDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("사용자 주소 목록 조회 중 오류 발생 - userId: {}, 오류: {}", jwtUserId, e.getMessage());
            throw e;
        }
    }

    // 주소 생성
    @Transactional
    public AddressResponseDto createAddress(AddressCreateRequestDto requestDto) {
        log.info("주소 생성 시작 - 주소: {}, 타입: {}", requestDto.getAddress(), requestDto.getType());

        try {
            Integer ownerId = convertUserIdToOwnerId(requestDto.getOwnerId());

            // 프론트엔드 타입을 ADDRESS_LABEL 코드로 변환
            String labelCode = convertTypeToLabelCode(requestDto.getType());
            log.info("주소 라벨 코드 변환: {} → {}", requestDto.getType(), labelCode);

            // 기본 주소 설정 시 기존 기본 주소 해제
            if (requestDto.getIsDefault()) {
                addressRepository.resetDefaultAddress(ownerId);
                log.info("기존 기본 주소 해제 - ownerId: {}", ownerId);
            }

            AddressEntity addressEntity = AddressEntity.builder()
                    .ownerId(ownerId)
                    .label(labelCode)
                    .alias(requestDto.getAlias())
                    .roadAddr(requestDto.getAddress())
                    .detailAddr(requestDto.getDetail())
                    .postCode(requestDto.getPostcode())
                    .latitude(requestDto.getLatitude())
                    .longitude(requestDto.getLongitude())
                    .isDefault(requestDto.getIsDefault() ? 1 : 0)
                    .build();

            AddressEntity savedAddress = addressRepository.save(addressEntity);
            log.info("주소 생성 완료 - ID: {}, ownerId: {}, 라벨: {}",
                    savedAddress.getId(), ownerId, labelCode);

            return convertToResponseDto(savedAddress);
        } catch (Exception e) {
            log.error("주소 생성 중 오류 발생 - 주소: {}, 타입: {}, 오류: {}",
                    requestDto.getAddress(), requestDto.getType(), e.getMessage());
            throw e;
        }
    }


    // 주소 수정
    @Transactional
    public AddressResponseDto updateAddress(Integer addressId, AddressUpdateRequestDto requestDto) {
        log.info("주소 수정 시작 - ID: {}, 타입: {}", addressId, requestDto.getType());

        try {
            Integer ownerId = convertUserIdToOwnerId(requestDto.getOwnerId());

            // 주소 존재 및 권한 확인
            AddressEntity addressEntity = addressRepository.findById(addressId)
                    .orElseThrow(() -> {
                        log.warn("주소를 찾을 수 없음 - ID: {}", addressId);
                        return new RuntimeException("주소를 찾을 수 없습니다");
                    });

            if (!addressEntity.getOwnerId().equals(ownerId)) {
                log.warn("주소 수정 권한 없음 - ID: {}, ownerId: {}, requestOwnerId: {}",
                        addressId, addressEntity.getOwnerId(), ownerId);
                throw new RuntimeException("주소 수정 권한이 없습니다");
            }

            // 프론트엔드 타입을 ADDRESS_LABEL 코드로 변환
            String labelCode = convertTypeToLabelCode(requestDto.getType());
            log.info("주소 라벨 코드 변환: {} → {}", requestDto.getType(), labelCode);

            // 기본 주소 설정 시 기존 기본 주소 해제
            if (requestDto.getIsDefault() && addressEntity.getIsDefault() != 1) {
                addressRepository.resetDefaultAddress(ownerId);
                log.info("기존 기본 주소 해제 후 새 기본 주소 설정 - ownerId: {}", ownerId);
            }

            // 주소 정보 업데이트
            addressEntity.setLabel(labelCode);
            addressEntity.setAlias(requestDto.getAlias());
            addressEntity.setRoadAddr(requestDto.getAddress());
            addressEntity.setDetailAddr(requestDto.getDetail());
            addressEntity.setPostCode(requestDto.getPostcode());
            addressEntity.setLatitude(requestDto.getLatitude());
            addressEntity.setLongitude(requestDto.getLongitude());
            addressEntity.setIsDefault(requestDto.getIsDefault() ? 1 : 0);

            AddressEntity updatedAddress = addressRepository.save(addressEntity);
            log.info("주소 수정 완료 - ID: {}, ownerId: {}, 라벨: {}",
                    updatedAddress.getId(), ownerId, labelCode);

            return convertToResponseDto(updatedAddress);
        } catch (Exception e) {
            log.error("주소 수정 중 오류 발생 - ID: {}, 타입: {}, 오류: {}",
                    addressId, requestDto.getType(), e.getMessage());
            throw e;
        }
    }

    // 주소 삭제
    @Transactional
    public void deleteAddress(Integer addressId, String jwtUserId) {
        log.info("주소 삭제 시작 - ID: {}, userId: {}", addressId, jwtUserId);

        try {
            Integer ownerId = convertUserIdToOwnerId(jwtUserId);

            // 주소 존재 및 권한 확인
            if (!addressRepository.existsByIdAndOwnerId(addressId, ownerId)) {
                log.warn("주소를 찾을 수 없거나 삭제 권한 없음 - ID: {}, ownerId: {}", addressId, ownerId);
                throw new RuntimeException("주소를 찾을 수 없거나 삭제 권한이 없습니다");
            }

            addressRepository.deleteById(addressId);
            log.info("주소 삭제 완료 - ID: {}, ownerId: {}", addressId, ownerId);
        } catch (Exception e) {
            log.error("주소 삭제 중 오류 발생 - ID: {}, userId: {}, 오류: {}", addressId, jwtUserId, e.getMessage());
            throw e;
        }
    }

    // 기본 주소 설정
    @Transactional
    public void setDefaultAddress(Integer addressId, String jwtUserId) {
        log.info("기본 주소 설정 시작 - ID: {}, userId: {}", addressId, jwtUserId);

        try {
            Integer ownerId = convertUserIdToOwnerId(jwtUserId);

            // 권한 검증
            if (!addressRepository.existsByIdAndOwnerId(addressId, ownerId)) {
                log.warn("주소를 찾을 수 없거나 권한 없음 - ID: {}, ownerId: {}", addressId, ownerId);
                throw new RuntimeException("주소를 찾을 수 없거나 권한이 없습니다");
            }

            // 기존 기본 주소 해제
            addressRepository.resetDefaultAddress(ownerId);

            // 새 기본 주소 설정
            addressRepository.setDefaultAddress(addressId, ownerId);

            log.info("기본 주소 설정 완료 - ID: {}, ownerId: {}", addressId, ownerId);
        } catch (Exception e) {
            log.error("기본 주소 설정 중 오류 발생 - ID: {}, userId: {}, 오류: {}", addressId, jwtUserId,
                    e.getMessage());
            throw e;
        }
    }


    // Entity -> ResponseDto 변환
    private AddressResponseDto convertToResponseDto(AddressEntity addressEntity) {

        return AddressResponseDto.builder()
                .id(addressEntity.getId())
                .type(convertLabelCodeToType(addressEntity.getLabel()))  // 공통코드명으로 변환
                .alias(addressEntity.getAlias())
                .address(addressEntity.getRoadAddr())
                .detail(addressEntity.getDetailAddr())
                .postcode(addressEntity.getPostCode())
                .latitude(addressEntity.getLatitude())
                .longitude(addressEntity.getLongitude())
                .isDefault(addressEntity.getIsDefault() == 1)
                .createdAt(addressEntity.getCreatedAt())
                .updatedAt(addressEntity.getUpdatedAt())
                .build();
    }


}
