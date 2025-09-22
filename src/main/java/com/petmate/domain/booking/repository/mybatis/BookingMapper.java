package com.petmate.domain.booking.repository.mybatis;

import com.petmate.domain.booking.dto.request.BookingCreateRequest;
import com.petmate.domain.booking.dto.request.BookingSearchRequest;
import com.petmate.domain.booking.dto.response.BookingResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookingMapper {

    // 예약 생성
    int insertBooking(BookingCreateRequest request);

    // 예약 상세 조회
    BookingResponseDto selectBookingDetail(@Param("id") Integer id);

    // 사용자별 예약 목록
    List<BookingResponseDto> selectBookingByUser(
            @Param("userId") Integer userId,
            @Param("request") BookingSearchRequest request
    );

    // 업체별 예약 목록
    List<BookingResponseDto> selectBookingByCompany(
            @Param("companyId") Integer companyId,
            @Param("request") BookingSearchRequest request
    );

    // 시간대별 예약 수 체크
    int countBookingInTimeSlot(
            @Param("productId") Integer productId,
            @Param("date") String date,
            @Param("startTime") String startTme,
            @Param("endTime") String endTime
    );

    // 종일 예약 수 체크
    int countAllDayBooking(
            @Param("productId") Integer productId,
            @Param("date") String date
    );

    // 예약상태 업데이트
    int updateBookingStatus(
            @Param("id") Integer id,
            @Param("status") String status);

    // 결제 상태 업데이트
    int updatePaymentStatus(
            @Param("id") Integer id,
            @Param("paymentStatus") String paymentStatus
    );

    // 디버깅용: 전체 예약 데이터 조회 (WHERE 조건 없음)
    List<BookingResponseDto> selectAllBookingsForDebug(
            @Param("request") BookingSearchRequest request
    );

}
