package com.petmate.domain.booking.service;

import com.petmate.domain.booking.dto.request.BookingCreateRequest;
import com.petmate.domain.booking.dto.request.BookingSearchRequest;
import com.petmate.domain.booking.dto.response.BookingResponseDto;
import com.petmate.domain.booking.repository.mybatis.BookingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookingService {

    private final BookingMapper bookingMapper;
    private final TimeSlotService timeSlotService;

    public BookingResponseDto createBooking(BookingCreateRequest request) {
        try {
            log.info("예약 생성 요청: {}", request);

            // 시간 슬롯 유효성
            boolean isValidTimeSlot = timeSlotService.validateTimeSlot(
                    request.getProductId(),
                    request.getStartDt(),
                    request.getEndDt()
            );

            if(!isValidTimeSlot) {
                return BookingResponseDto.fail("선택한 시간은 예약할 수 없습니다");
            }

            // 시간 슬롯 예약 가능 여부 체크
            boolean isAvailable = timeSlotService.isTimeSlotAvailable(
                    request.getProductId(),
                    request.getStartDt(),
                    request.getEndDt()
            );

            if(!isAvailable) {
                return BookingResponseDto.fail("선택한 시간은 이미 예약이 있습니다.");
            }

            // 예약 생성
            int result = bookingMapper.insertBooking(request);
            if(result > 0) {
                log.info("예약 생성 성공 : id={}", request.getId());
                return BookingResponseDto.builder()
                        .success(true)
                        .message("예약이 성공적으로 생성되었습니다.")
                        .id(request.getId())
                        .build();
            } else {
                return BookingResponseDto.fail("예약 생서에 실패하였습니다.");
            }
        } catch (Exception e) {
            log.error("예약 조회 중 오류 발생", e);
            return BookingResponseDto.fail("예약 생성 중 오류 발생!!");
        }
    }

    public Optional<BookingResponseDto> getBookingDetail(Integer id) {
        try {
            BookingResponseDto booking = bookingMapper.selectBookingDetail(id);
            return Optional.ofNullable(booking);
        } catch (Exception e) {
            log.error("예약 조회 중 오류 발생 : id={}", id, e);
            return Optional.empty();
        }
    }
    public List<BookingResponseDto> getBookingByUser(Integer userId, BookingSearchRequest request) {
        try {
            return bookingMapper.selectBookingByUser(userId, request);
        } catch (Exception e) {
            log.error("사용자 예약 목록 조회 중 오류 발생: userId={}", userId, e);
            return List.of();
        }
    }

    public List<BookingResponseDto> getBookingByCompany(Integer companyId, BookingSearchRequest request) {
        try {
            return bookingMapper.selectBookingByCompany(companyId, request);
        } catch (Exception e) {
            log.error("업체 예약 목록 조회 중 오류 발생 : companyId={}", companyId, e);
            return List.of();
        }

    }

    public BookingResponseDto updateBookingStatus(Integer id, String status) {
        try {
            int result = bookingMapper.updateBookingStatus(id, status);

            if(result > 0) {
                log.info("예약상태 변경 성공 : id={}, status={}", id, status);
                return BookingResponseDto.success("예약상태가 변경 되었습니다.");
            } else {
                return BookingResponseDto.fail("예약 상태 변경에 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("예약 상태 변경 중 오류 발생: ID = {}, status = {}", id, status, e);
            return BookingResponseDto.fail("예약 상태 변경 중 오류가 발생했습니다.");
        }
    }

    public BookingResponseDto updatePaymentStatus(Integer id, String paymentStatus) {
        try {
            int result = bookingMapper.updatePaymentStatus(id, paymentStatus);

            if (result > 0) {
                log.info("결제 상태 변경 성공: reservationId = {}, paymentStatus = {}", id, paymentStatus);
                return BookingResponseDto.success("결제 상태가 변경되었습니다.");
            } else {
                return BookingResponseDto.fail("결제 상태 변경에 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("결제 상태 변경 중 오류 발생: ID = {}, paymentStatus = {}", id, paymentStatus, e);
            return BookingResponseDto.fail("결제 상태 변경 중 오류가 발생했습니다.");
        }
    }

    public BookingResponseDto cancelReservation(Integer id) {
        return updateBookingStatus(id, "3"); // 3 = 예약취소
    }

    public BookingResponseDto confirmReservation(Integer id) {
        return updateBookingStatus(id, "1"); // 1 = 예약확정
    }

}
