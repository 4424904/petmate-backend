package com.petmate.domain.address.controller;

import com.petmate.domain.address.dto.request.AddressCreateRequestDto;
import com.petmate.domain.address.dto.request.AddressUpdateRequestDto;
import com.petmate.domain.address.dto.response.AddressResponseDto;
import com.petmate.domain.address.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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


    // 주소 목록 조회
    @GetMapping
    public ResponseEntity<List<AddressResponseDto>> getAddresses(
            @AuthenticationPrincipal String userId
    ) {

        log.info("사용자 주소 목록 조회 요청 - userId: {}", userId);

        try {
            List<AddressResponseDto> addresses =
                    addressService.getUserAddresses(userId);

            log.info("사용자 주소 목록 조회 완료 - userId: {}, 주소 개수: {}",
                    userId, addresses.size());

            return ResponseEntity.ok(addresses);

        } catch (RuntimeException e) {
            log.error("사용자 주소 목록 조회 실패 - userId: {}, 오류: {}",
                    userId, e.getMessage());
            throw e;
        }
    }


    // 주소 추가
    @PostMapping
    public ResponseEntity<AddressResponseDto> createAddress(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody AddressCreateRequestDto addressCreateRequestDto
    ) {

        log.info("주소 추가 요청 - userId: {}, 주소: {}",
                userId, addressCreateRequestDto.getAddress());

        log.info("받은 위도/경도 - latitude: {}, longitude: {}", 
                addressCreateRequestDto.getLatitude(), addressCreateRequestDto.getLongitude());

        try {
            // String userId를 DTO에 설정 (Service에서 Integer로 변환)
            addressCreateRequestDto.setOwnerId(userId);

            AddressResponseDto createdAddress =
                    addressService.createAddress(addressCreateRequestDto);

            log.info("주소 추가 완료 - userId: {}, 주소 ID: {}",
                    userId, createdAddress.getId());

            return
                    ResponseEntity.ok(createdAddress);

        } catch (RuntimeException e) {
            log.error("주소 추가 실패 - userId: {}, 오류: {}",
                    userId, e.getMessage());
            throw e;
        }


    }

    // 주소 수정
    @PutMapping("/{id}")
    public ResponseEntity<AddressResponseDto> updateAddress(
            @AuthenticationPrincipal String userId,
            @PathVariable Integer id,
            @Valid @RequestBody AddressUpdateRequestDto addressUpdateRequestDto
    ) {

        log.info("주소 수정 요청 - userId: {}, 주소 ID: {}", userId, id);

        try {
            // String userId를 DTO에 설정 (Service에서 Integer로 변환)
            addressUpdateRequestDto.setOwnerId(userId);

            AddressResponseDto updatedAddress =
                    addressService.updateAddress(id, addressUpdateRequestDto);

            log.info("주소 수정 완료 - userId: {}, 주소 ID: {}", userId, id);

            return ResponseEntity.ok(updatedAddress);

        } catch (RuntimeException e) {
            log.error("주소 수정 실패 - userId: {}, 주소 ID: {}, 오류: {}",
                    userId, id, e.getMessage());
            throw e;
        }

    }

    // 주소 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal String userId,
            @PathVariable Integer id
    ) {

        log.info("주소 삭제 요청 - userId: {}, 주소 ID: {}", userId, id);

        try {
            addressService.deleteAddress(id, userId);

            log.info("주소 삭제 완료 - userId: {}, 주소 ID: {}", userId, id);

            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            log.error("주소 삭제 실패 - userId: {}, 주소 ID: {}, 오류: {}",
                    userId, id, e.getMessage());
            throw e;
        }

    }

    // 기본 주소 설정
    @PutMapping("/{id}/default")
    public ResponseEntity<Void> setDefaultAddress(
            @AuthenticationPrincipal String userId,
            @PathVariable Integer id
    ) {

        log.info("기본 주소 설정 요청 - userId: {}, 주소 ID: {}", userId, id);

        try {
            addressService.setDefaultAddress(id, userId);

            log.info("기본 주소 설정 완료 - userId: {}, 주소 ID: {}", userId, id);

            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            log.error("기본 주소 설정 실패 - userId: {}, 주소 ID: {}, 오류: {}",
            userId, id, e.getMessage());
            throw e;
        }

    }



}
