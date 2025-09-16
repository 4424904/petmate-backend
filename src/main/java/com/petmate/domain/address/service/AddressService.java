package com.petmate.domain.address.service;

import com.petmate.domain.address.dto.request.AddressCreateRequestDto;
import com.petmate.domain.address.dto.request.AddressUpdateRequestDto;
import com.petmate.domain.address.dto.response.AddressResponseDto;
import com.petmate.domain.address.entity.AddressEntity;
import com.petmate.domain.address.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AddressService {

    private final AddressRepository addressRepository;
//    private final CodeService codeService; (codeService 구현 후 주입 예정)

    private String convertTypeToLabelCode(String type) {
        switch (type) {
            case "home": return "1";
            case "office": return "2";
            case "etc": return "3";
            default: throw new IllegalArgumentException("유효하지 않은 주소 유형: " + type);
        }
    }

    private String convertLabelCodeToType(String labelCode) {
        switch (labelCode) {
            case "1": return "home";
            case "2": return "office";
            case "3": return "etc";
            default: return "etc";
        }
    }

    // jwt String userId를 integer ownerId로 변환
    private Integer convertUserIdToOwnerId(String jwtUserId) {
        try{
            log.info("convertUserIdToOwnerId 입력값: '{}'", jwtUserId);
            // User DB의 id를 그대로 ownerId로 사용
            Integer result = Integer.parseInt(jwtUserId);
            log.info("convertUserIdToOwnerId 결과값: {}", result);
            return result;
        } catch (Exception e) {
            log.error("JWT userId를 Integer로 변환 실패: {}", jwtUserId);
            throw new RuntimeException("유효하지 않은 사용자 아이디입니다");
        }
    }

    // 사용자 주소 목록 조회
    @Transactional(readOnly = true)
    public List<AddressResponseDto> getUserAddresses(String jwtUserId) {
        Integer ownerId = convertUserIdToOwnerId(jwtUserId);

        List<AddressEntity> address = addressRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);

        log.info("사용자 주소 목록 조회 완료 - ownerId: {}, 개수: {}", ownerId, address.size());

        return address.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    // 주소 생성
    @Transactional
    public AddressResponseDto createAddress(AddressCreateRequestDto requestDto) {
        Integer ownerId = convertUserIdToOwnerId(requestDto.getOwnerId());

        // 프론트엔드 타입을 DB 라벨 코드로 변환
        String labelCode = convertTypeToLabelCode(requestDto.getType());

        // 기본 주소 설정 시 기존 기본 주소 해제
        if (requestDto.getIsDefault()) {
            addressRepository.resetDefaultAddress(ownerId);
            log.info("기존 기본 주소 해제 - ownerId: {}", ownerId);
        }

        // 엔티티 생성 및 저장
        AddressEntity addressEntity = AddressEntity.builder()
                .ownerId(ownerId)
                .label(labelCode)
                .roadAddr(requestDto.getAddress())
                .detailAddr(requestDto.getDetail())
                .alias(requestDto.getAlias())
                .postCode(requestDto.getPostcode())
                .latitude(requestDto.getLatitude())
                .longitude(requestDto.getLongitude())
                .isDefault(requestDto.getIsDefault() ? 1 : 0)
                .build();

        AddressEntity savedAddressEntity = addressRepository.save(addressEntity);

        log.info("주소 생성 완료 - id: {}, ownerId: {}", savedAddressEntity.getId(), ownerId);

        return convertToResponseDto(savedAddressEntity);
    }


    // 주소 수정
    @Transactional
    public AddressResponseDto updateAddress(Integer addressId, AddressUpdateRequestDto requestDto) {
        Integer ownerId = convertUserIdToOwnerId(requestDto.getOwnerId());

        // 주소 존재 및 권한 확인
        AddressEntity addressEntity = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("주소를 찾을 수 없습니다"));

        if(!addressEntity.getOwnerId().equals(ownerId)) {
            throw new RuntimeException("주소 수정 권한이 없습니다");
        }

        // 프론트엔드 타입을 DB 라벨 코드로 변환
        String labelCode = convertTypeToLabelCode(requestDto.getType());

        // 기본 주소로 변경 시 기존 기본 주소 해제
        if(requestDto.getIsDefault() && addressEntity.getIsDefault() != 1) {
            addressRepository.resetDefaultAddress(ownerId);
            log.info("기존 기본 주소 해제 - ownerId: {}", ownerId);
        }
        
        // 엔티티 정보 업데이트
        AddressEntity updateAddressEntity = AddressEntity.builder()
                .id(addressEntity.getId())
                .ownerId(addressEntity.getOwnerId())
                .label(labelCode)
                .roadAddr(requestDto.getAddress())
                .detailAddr(requestDto.getDetail())
                .alias(requestDto.getAlias())
                .postCode(requestDto.getPostcode())
                .latitude(requestDto.getLatitude())
                .longitude(requestDto.getLongitude())
                .isDefault(requestDto.getIsDefault() ? 1 : 0)
                .createdAt(addressEntity.getCreatedAt())
                .build();

        AddressEntity savedAddressEntity = addressRepository.save(updateAddressEntity);

        log.info("주소 수정 완료 - id: {}, ownerId: {}", addressId, ownerId);

        return convertToResponseDto(savedAddressEntity);
    }

    // 주소 삭제
    @Transactional
    public void deleteAddress(Integer addressId, String jwtUserId) {
        Integer ownerId = convertUserIdToOwnerId(jwtUserId);

        // 주소 존재 및 권한 확인
        if (!addressRepository.existsByIdAndOwnerId(addressId, ownerId)) {
            throw new RuntimeException("주소를 찾을 수 없거나 삭제 권한이 없습니다");
        }

        // 수동 삭제
        addressRepository.deleteById(addressId);
        log.info("주소 삭제 완료 - id: {}, ownerId: {}", addressId, ownerId);
    }

    // 기본 주소 설정
    @Transactional
    public void setDefaultAddress(Integer addressId, String jwtUserId) {
        Integer ownerId = convertUserIdToOwnerId(jwtUserId);

        // 권한 검증
        if(!addressRepository.existsByIdAndOwnerId(addressId, ownerId)) {
            throw new RuntimeException("주소를 찾을 수 없거나 권한이 없습니다");
        }

        // 기존 기본 주소 해제
        addressRepository.resetDefaultAddress(ownerId);

        // 새 기본 주소 설정
        addressRepository.setDefaultAddress(addressId, ownerId);

        log.info("기본 주소 설정 완료 - id: {}, ownerId: {}", addressId, ownerId);
    }


    // Entity -> ResponseDto 변환
    private AddressResponseDto convertToResponseDto(AddressEntity addressEntity) {
        String frontendType = convertLabelCodeToType(addressEntity.getLabel());

        return AddressResponseDto.builder()
                .id(addressEntity.getId())
                .type(frontendType)
                .address(addressEntity.getRoadAddr())
                .detail(addressEntity.getDetailAddr())
                .alias(addressEntity.getAlias())
                .isDefault(addressEntity.getIsDefault() == 1)
                .postcode(addressEntity.getPostCode())
                .latitude(addressEntity.getLatitude())
                .longitude(addressEntity.getLongitude())
                .createdAt(addressEntity.getCreatedAt())
                .updatedAt(addressEntity.getUpdatedAt())
                .build();
    }


}
