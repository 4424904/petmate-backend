package com.petmate.domain.booking.service;

import com.petmate.domain.booking.dto.OperatingHours;
import com.petmate.domain.booking.dto.response.TimeSlotResponse;
import com.petmate.domain.booking.repository.mybatis.BookingMapper;
import com.petmate.domain.booking.util.OperatingHoursParser;
import com.petmate.domain.company.dto.response.CompanyResponseDto;
import com.petmate.domain.company.entity.CompanyEntity;
import com.petmate.domain.company.service.CompanyService;
import com.petmate.domain.product.dto.response.ProductResponseDto;
import com.petmate.domain.product.entity.ProductEntity;
import com.petmate.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimeSlotService {

    private final ProductService productService;
    private final CompanyService companyService;
    private final BookingMapper bookingMapper;
    private final OperatingHoursParser operatingHoursParser;

    public List<TimeSlotResponse> getAvailableTimeSlots(Integer productId, String dateStr) {

        try {
            LocalDate date = LocalDate.parse(dateStr);

            // 과거 날짜 예약 금지
            if(date.isBefore(LocalDate.now())) {
                return Collections.emptyList();
            }

            // 상품 정보를 조회
            ProductResponseDto product = productService.getProduct(productId);
            if(product == null || product.getIsActive() != 1) {
                return Collections.emptyList();
            }

            // 업체정보 조회
            CompanyResponseDto company = companyService.getCompanyByIdPublic(product.getCompanyId());
            if (company == null) {
                return Collections.emptyList();
            }

            // 운영시간
            OperatingHours dayHours = operatingHoursParser.parseOperatingHours(
                    company.getOperatingHours(), date
            );
            if(dayHours == null || dayHours.isClosed()) {
                return Collections.emptyList();
            }

            // 종일 서비스
            if(product.getAllDay() == 1) {
                return createAllDaySlot(product, dayHours, date);
            }

            return createTimeSlots(product, dayHours, date);

        } catch (Exception e) {
            log.error("시간 슬롯 생성 오류 : productId={}, date={}", productId, dateStr, e);
            return Collections.emptyList();
        }
    }

    public boolean validateTimeSlot(Integer productId, LocalDateTime startDt, LocalDateTime endDt) {
        try {
            if(startDt == null || endDt == null) {
                return false;
            }

            // 시작시간이 종료시간보다 늦으면 x
            if(!startDt.isBefore(endDt)) {
                return false;
            }

            // 과거 시간 예약 불가
            if(startDt.isBefore(LocalDateTime.now())) {
                return false;
            }

            // 상품 정보 조회
            ProductResponseDto product = productService.getProduct(productId);
            if(product == null) {
                return false;
            }

            // 업체 정보 조회
//            CompanyResponseDto company = companyService.getCompanyById(product.getCompanyId(), null);
            CompanyResponseDto company = companyService.getCompanyByIdPublic(product.getCompanyId());
            if(company == null) {
                return false;
            }

            // 운영시간 인지
            OperatingHours dayHours = operatingHoursParser.parseOperatingHours(
                    company.getOperatingHours(), startDt.toLocalDate()
            );
            if(dayHours == null || dayHours.isClosed()) {
                return false;
            }

            LocalTime startTime = startDt.toLocalTime();
            LocalTime endTime = endDt.toLocalTime();

            return !startTime.isBefore(dayHours.getStartTime()) &&
                    !endTime.isAfter(dayHours.getEndTime());

        } catch (Exception e) {
            log.error("시간 슬롯 검증 오류", e);
            return false;
        }
    }

    public boolean isTimeSlotAvailable(Integer productId, LocalDateTime startDt, LocalDateTime endDt) {
        try {
            int currentBookings = bookingMapper.countBookingInTimeSlot(
                    productId,
                    startDt.toLocalDate().toString(),
                    startDt.toLocalTime().toString(),
                    endDt.toLocalTime().toString()
            );

            // 1개만 예약허용하기
            return currentBookings == 0;
        } catch (Exception e) {
            log.error("시간 슬롯 가용성 체크 오류", e);
            return false;
        }
    }

    private List<TimeSlotResponse> createTimeSlots(ProductResponseDto product, OperatingHours dayHours, LocalDate date) {
        List<TimeSlotResponse> slots = new ArrayList<>();

        LocalTime startTime = dayHours.getStartTime();
        LocalTime endTime = dayHours.getEndTime();
        int durationMin = product.getDurationMin() != null ? product.getDurationMin() : 60;

        LocalTime currentTime = startTime;

        while (currentTime.plusMinutes(durationMin).isBefore(endTime) ||
                currentTime.plusMinutes(durationMin).equals(endTime)) {
            LocalTime slotEndTime = currentTime.plusMinutes(durationMin);

            // 과거 슬롯 제외
            LocalDateTime slotDateTime = LocalDateTime.of(date, currentTime);
            if(slotDateTime.isBefore(LocalDateTime.now())) {
                currentTime = currentTime.plusMinutes(durationMin);
                continue;
            }

            // 예약 현황 체크
            int currentBookings = bookingMapper.countBookingInTimeSlot(
                    product.getId(),
                    date.toString(),
                    currentTime.toString(),
                    slotEndTime.toString()
            );

            boolean isAvailable = currentBookings == 0;

            slots.add(TimeSlotResponse.builder()
                    .startTime(currentTime)
                    .endTime(slotEndTime)
                    .isAvailable(isAvailable)
                    .maxBookings(1)
                    .price(product.getPrice())
                    .isAllDay(false)
                    .build());

            currentTime = currentTime.plusMinutes(durationMin);
        }
        return slots;
    }

    private List<TimeSlotResponse> createAllDaySlot(ProductResponseDto product, OperatingHours dayHours, LocalDate date) {

        // 과거날짜 예약 불가
        if(date.isBefore(LocalDate.now())) {
            return Collections.emptyList();
        }

        int currentBookings = bookingMapper.countAllDayBooking(
                product.getId(),
                date.toString()
        );

        return Arrays.asList(TimeSlotResponse.builder()
                        .startTime(dayHours.getStartTime())
                        .endTime(dayHours.getEndTime())
                        .isAvailable(currentBookings == 0)
                        .currentBookings(currentBookings)
                        .maxBookings(1)
                        .price(product.getPrice())
                        .isAllDay(true)
                        .build());

    }


}
