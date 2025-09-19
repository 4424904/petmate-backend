package com.petmate.domain.booking.controller;

import com.petmate.domain.booking.dto.response.TimeSlotResponse;
import com.petmate.domain.booking.service.TimeSlotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
public class TimeSlotController {

    private final TimeSlotService timeSlotService;
    
    // 상품별 예약가능 조회
    @GetMapping("/{productId}/available-slots")
//    @PreAuthorize("permitAll()")
    public ResponseEntity<List<TimeSlotResponse>> getAvailableTimeSlots(
            @PathVariable Integer productId,
            @RequestParam String date 
    ) {
        log.info("시간 슬롯 조회 요청 : productId={}, date={}", productId, date);
        
        // 날짜 형식 검증
        try {
            LocalDate.parse(date);
            
        } catch (DateTimeParseException e) {
            log.error("잘못된 날짜 형식: {}", date);
            return ResponseEntity.badRequest().body(List.of());
        }
        
        // 상품 id 검증
        if(productId == null || productId <= 0) {
            log.error("잘못된 상품 id:{}", productId);
            return ResponseEntity.badRequest().body(List.of());
        }
        
        try {
            List<TimeSlotResponse> availableSlots = timeSlotService.getAvailableTimeSlots(productId, date);
            log.info("시간 슬롯 조회 성공 {}개", availableSlots.size());
            return ResponseEntity.ok(availableSlots);
        } catch (Exception e) {
            log.error("시간 슬롯 조회 중 오류 발생", e);
            return ResponseEntity.ok(List.of()); // 빈 목록을 반환
        }
    }

    // 시간 새로고침
    @PostMapping("/{productId}/refresh-slots")
    public ResponseEntity<List<TimeSlotResponse>> refreshTimeSlots(
            @PathVariable Integer productId,
            @RequestParam String date
    ) {
        log.info("시간 슬롯 새로고침 요청: productId={}, date={}", productId, date);
        return getAvailableTimeSlots(productId, date);
    }
}
