package com.petmate.domain.address.controller;

import com.petmate.domain.address.dto.request.AddressCreateRequestDto;
import com.petmate.domain.address.dto.request.AddressUpdateRequestDto;
import com.petmate.domain.address.dto.response.AddressResponseDto;
import com.petmate.domain.address.entity.AddressEntity;
import com.petmate.domain.address.service.AddressService;
import com.petmate.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
@CrossOrigin(
        origins = "http://localhost:3000",
        allowCredentials = "true"
)
@Slf4j
public class AddressController {

    private final AddressService addressService;

    // 현재 로그인된 사용자 ID 추출
    private String getUserId(Authentication authentication) {
        if (authentication == null) return null;

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails customUser) {
            return String.valueOf(customUser.getId()); // DB의 user PK(ID) 반환
        } else if (principal instanceof org.springframework.security.core.userdetails.User user) {
            return user.getUsername(); // 일반 Spring UserDetails
        } else if (principal instanceof String str) {
            return str; // 혹시 문자열로만 들어온 경우
        }

        return null;
    }

    // 주소 목록 조회
    @GetMapping
    public ResponseEntity<List<AddressResponseDto>> getAddresses(
            Authentication authentication,
            @RequestParam(required = false) Double userLat,
            @RequestParam(required = false) Double userLng
    ) {
        String userId = getUserId(authentication);
        log.info("사용자 주소 목록 조회 요청 - userId: {}, userLat: {}, userLng: {}", userId, userLat, userLng);

        List<AddressResponseDto> addresses;

        if(userLat != null && userLng != null) {

            addresses = addressService.getAddressesWithDistance(Integer.valueOf(userId), userLat, userLng);
            log.info("거리 계산 포함 주소 목록 조회 완료 - userId: {}, 주소 개수: {}", userId, addresses.size());

        } else {
            addresses = addressService.getUserAddresses(userId);
            log.info("기본 주소 목록 조회 완료 - userId: {}, 주소 개수: {}", userId, addresses.size());
        }

        return ResponseEntity.ok(addresses);
    }

    // 주소 추가
    @PostMapping
    public ResponseEntity<AddressResponseDto> createAddress(
            Authentication authentication,
            @Valid @RequestBody AddressCreateRequestDto addressCreateRequestDto
    ) {
        String userId = getUserId(authentication);
        log.info("주소 추가 요청 - userId: {}, 주소: {}", userId, addressCreateRequestDto.getAddress());

        addressCreateRequestDto.setOwnerId(userId);
        AddressResponseDto createdAddress = addressService.createAddress(addressCreateRequestDto);

        log.info("주소 추가 완료 - userId: {}, 주소 ID: {}", userId, createdAddress.getId());
        return ResponseEntity.ok(createdAddress);
    }

    // 주소 수정
    @PutMapping("/{id}")
    public ResponseEntity<AddressResponseDto> updateAddress(
            Authentication authentication,
            @PathVariable Integer id, // Integer 유지
            @Valid @RequestBody AddressUpdateRequestDto addressUpdateRequestDto
    ) {
        String userId = getUserId(authentication);
        log.info("주소 수정 요청 - userId: {}, 주소 ID: {}", userId, id);

        addressUpdateRequestDto.setOwnerId(userId);
        AddressResponseDto updatedAddress = addressService.updateAddress(id, addressUpdateRequestDto);

        log.info("주소 수정 완료 - userId: {}, 주소 ID: {}", userId, id);
        return ResponseEntity.ok(updatedAddress);
    }

    // 주소 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            Authentication authentication,
            @PathVariable Integer id // Integer 유지
    ) {
        String userId = getUserId(authentication);
        log.info("주소 삭제 요청 - userId: {}, 주소 ID: {}", userId, id);

        addressService.deleteAddress(id, userId);
        log.info("주소 삭제 완료 - userId: {}, 주소 ID: {}", userId, id);

        return ResponseEntity.ok().build();
    }

    // 기본 주소 설정
    @PutMapping("/{id}/default")
    public ResponseEntity<Void> setDefaultAddress(
            Authentication authentication,
            @PathVariable Integer id // Integer 유지
    ) {
        String userId = getUserId(authentication);
        log.info("기본 주소 설정 요청 - userId: {}, 주소 ID: {}", userId, id);

        addressService.setDefaultAddress(id, userId);
        log.info("기본 주소 설정 완료 - userId: {}, 주소 ID: {}", userId, id);

        return ResponseEntity.ok().build();
    }

    // 주소 목록 조회
    @GetMapping("/{id}")
    public ResponseEntity<AddressEntity> getUserAddressesByDefault(Authentication authentication) {
        String userId = getUserId(authentication);
        log.info("사용자 기본주소 조회 요청 - userId: {}", userId);

        AddressEntity addresses = addressService.getUserAddressesByDefault(userId);
        log.info("사용자 기본주소 조회 완료 - userId: {}", userId);

        return ResponseEntity.ok(addresses);
    }


}
