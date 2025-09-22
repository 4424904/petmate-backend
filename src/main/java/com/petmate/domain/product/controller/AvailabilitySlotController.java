package com.petmate.domain.product.controller;

import com.petmate.domain.product.dto.request.AvailabilitySlotBulkCreateRequest;
import com.petmate.domain.product.dto.request.AvailabilitySlotCreateRequest;
import com.petmate.domain.product.dto.response.AvailabilitySlotResponseDto;
import com.petmate.domain.product.service.AvailabilitySlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/availabilityslot")
@RequiredArgsConstructor
public class AvailabilitySlotController {

    private final AvailabilitySlotService slotService;

    // 예약 가능 조회
    @GetMapping("/available")
    public ResponseEntity<List<AvailabilitySlotResponseDto>> getAbailableSlots(
            @RequestParam Integer productId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)LocalDate date) {

        List<AvailabilitySlotResponseDto> responseDto = slotService.getAvailableSlots(productId, date);
        return ResponseEntity.ok(responseDto);
    }

    // 단일 슬롯 생성
    @PostMapping
    public ResponseEntity<AvailabilitySlotResponseDto> createSlot(@RequestParam AvailabilitySlotCreateRequest request) {
        AvailabilitySlotResponseDto responseDto = slotService.createSlot(request);
        return ResponseEntity.ok(responseDto);
    }

    // 대량 슬롯 생성(기간별)
    @PostMapping("/bulk")
    public ResponseEntity<List<AvailabilitySlotResponseDto>> createBulkSlots(@RequestBody AvailabilitySlotBulkCreateRequest request) {
        List<AvailabilitySlotResponseDto> responseDto = slotService.createBulkSlots(request);
        return ResponseEntity.ok(responseDto);
    }

    // 업체의 모든 슬롯 조회
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<AvailabilitySlotResponseDto>> getSlotByCompany(@PathVariable Integer companyId) {
        List<AvailabilitySlotResponseDto> responseDto = slotService.getSlotByCompany(companyId);
        return ResponseEntity.ok(responseDto);
    }

    // 개별 슬롯 삭제
    @DeleteMapping("/{slotId}")
    public ResponseEntity<Void> deleteSlot(@PathVariable Integer slotId) {
        slotService.deleteSlot(slotId);
        return ResponseEntity.noContent().build();
    }

    // 상품의 모든 슬롯 삭제
    @DeleteMapping("/product/{productId}")
    public ResponseEntity<Map<String, Object>> deleteSlotsByProductId(@PathVariable Integer productId) {
        Map<String, Object> result = slotService.deleteSlotsByProductId(productId);
        return ResponseEntity.ok(result);
    }

    // 상품 슬롯 정보 조회
    @GetMapping("/product/{productId}/info")
    public ResponseEntity<Map<String, Object>> getProductSlotInfo(@PathVariable Integer productId) {
        Map<String, Object> info = slotService.getProductSlotInfo(productId);
        return ResponseEntity.ok(info);
    }

}
