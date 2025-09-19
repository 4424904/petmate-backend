package com.petmate.domain.booking.controller;

import com.petmate.domain.booking.dto.request.BookingCreateRequest;
import com.petmate.domain.booking.dto.request.BookingSearchRequest;
import com.petmate.domain.booking.dto.response.BookingResponseDto;
import com.petmate.domain.booking.entity.BookingEntity;
import com.petmate.domain.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/booking")
@RequiredArgsConstructor
//@CrossOrigin(origins = "*") // 모든 도메인 허용
@Slf4j
public class BookingController {

    private final BookingService bookingService;

    // 예약 생성
    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(
            @Valid @RequestBody BookingCreateRequest request
            ) {
        log.info("== 예약 생성 수신 ==");
        log.info("요청 데이터 {}", request);

        try {
            BookingResponseDto responseDto = bookingService.createBooking(request);

            if(responseDto.isSuccess()) {
                log.info("예약 생성 성공!(201) id={}", responseDto.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
            } else {
                log.warn("예약 생성 실패!(400) {}", responseDto.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDto);
            }
        } catch (Exception e) {
            log.error("==예약 생성 중 예외 발생==", e);
            BookingResponseDto errResponseDto = BookingResponseDto.fail("서버 오류 발생!");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errResponseDto);
        }
    }

    // 예약 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponseDto> getBookingByUser(
            @PathVariable Integer id) {
        log.info("예약 상세 조회 요청 id={}", id);

        return bookingService.getBookingDetail(id)
                .map(ResponseEntity::ok) // .map(booking -> ResponseEntity.ok(booking))
                .orElse(ResponseEntity.notFound().build());
    }

    // 사용자별 예약 목록 조회하기
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponseDto>> getBookingByUser(
            @PathVariable Integer userId,
            @ModelAttribute BookingSearchRequest request
    ) {
        log.info("사용자 예약 목록 조회 : userId={}", userId);

        List<BookingResponseDto> bookings = bookingService.getBookingByUser(userId, request);
        return ResponseEntity.ok(bookings);
    }

    // 업체별 예약 목록 조회
    @GetMapping("/company/{compayId}")
    public ResponseEntity<List<BookingResponseDto>> getBookingByCompany(
            @PathVariable Integer companyId,
            @ModelAttribute BookingSearchRequest request
    ) {
        log.info("업체 예약 목록 조회: companyId={}, request={}", companyId, request);

        List<BookingResponseDto> bookings = bookingService.getBookingByCompany(companyId, request);
        return ResponseEntity.ok(bookings);
    }

    // 예약 상태 변경
    @PostMapping("/{id}/status")
    public ResponseEntity<BookingResponseDto> updateBookingStatus(
            @PathVariable Integer id,
            @RequestParam String status
    ) {
        log.info("예약 상태 변경 요청 : id={}, status={}", id, status);

        // 상태 값 검증
        if(!isValidStatus(status)) {
            BookingResponseDto errResponse = BookingResponseDto.fail("올바르지 않은 상태 값");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errResponse);
        }

        try {
            BookingResponseDto responseDto = bookingService.updateBookingStatus(id, status);

            if(responseDto.isSuccess()) {
                return ResponseEntity.ok(responseDto);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDto);
            }
        } catch (Exception e) {
            log.error("예약 상태 변경 중 오류", e);
            BookingResponseDto errResponse = BookingResponseDto.fail("상태 변경 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errResponse);
        }
    }

    // 예약 상태 유효성 검사
    private boolean isValidStatus(String status) {
        return  status != null && status.matches("^[0-3]$");
    }

    // 예약 취소
    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponseDto> cancelBooking(@PathVariable Integer id) {
        log.info("예약 취소 요청 : id={}", id);

        try {
            BookingResponseDto responseDto = bookingService.cancelReservation(id);

            if (responseDto.isSuccess()) {
                return ResponseEntity.ok(responseDto);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDto);
            }
        } catch (Exception e) {
            log.error("예액 취소 중 오류 발생", e);
            BookingResponseDto errResponse = BookingResponseDto.fail("예약 취소중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errResponse);
        }

    }

    // 예약 확정(업체용)
    @PutMapping("/{id}/confirm")
    public ResponseEntity<BookingResponseDto> confirmBooking(@PathVariable Integer id) {
        log.info("예약 확정 요청(업체용) : id={}", id);

        try {
            BookingResponseDto responseDto = bookingService.confirmReservation(id);

            if(responseDto.isSuccess()) {
                return ResponseEntity.ok(responseDto);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseDto);
            }
        } catch (Exception e) {
            log.error("예약 확정 중 오류 발생", e);
            BookingResponseDto errResponse = BookingResponseDto.fail("예약 확정 중 오류가 발생했습니다");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errResponse);
        }
    }

    // 결제용 예약 정보 조회 (인증 불필요)
    @GetMapping("/payment/{id}")
    public ResponseEntity<BookingResponseDto> getBookingForPayment(@PathVariable Integer id) {
        log.info("결제용 예약 정보 조회 요청 id={}", id);

        return bookingService.getBookingDetail(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}
