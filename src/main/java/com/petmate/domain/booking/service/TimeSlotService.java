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
            log.info("=== 시간 슬롯 생성 시작 ===");
            LocalDate date = LocalDate.parse(dateStr);

            // 과거 날짜 예약 금지
            if(date.isBefore(LocalDate.now())) {
                log.info("과거 날짜 예약 금지: {}", date);
                return Collections.emptyList();
            }

            // 상품 정보를 조회
            log.info("상품 정보 조회 시작: productId={}", productId);
            ProductResponseDto product = productService.getProduct(productId);
            log.info("상품 정보 조회 완료: {}", product != null ? "성공" : "실패");
            if(product == null || product.getIsActive() != 1) {
                return Collections.emptyList();
            }

            // 업체정보 조회
            log.info("업체 정보 조회 시작: companyId={}", product.getCompanyId());
            CompanyResponseDto company = companyService.getCompanyByIdPublic(product.getCompanyId());
            log.info("업체 정보 조회 완료: {}", company != null ? "성공" : "실패");
            if (company == null) {
                return Collections.emptyList();
            }

            // 운영시간
            OperatingHours dayHours = operatingHoursParser.parseOperatingHours(
                    company.getOperatingHours(), date
            );
            log.info("운영시간 파싱 결과: {}", dayHours != null ?
                (dayHours.isClosed() ? "휴무일" : "영업일 " + dayHours.getStartTime() + "-" + dayHours.getEndTime()) : "null");

            if(dayHours == null || dayHours.isClosed()) {
                log.info("운영시간이 null이거나 휴무일로 인해 빈 목록 반환");
                return Collections.emptyList();
            }

            // 종일 서비스
            if(product.getAllDay() == 1) {
                log.info("종일 서비스로 처리");
                return createAllDaySlot(product, dayHours, date);
            }

            log.info("일반 시간 슬롯 생성 시작");
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
        // 임시로 항상 true 반환 (예약 가능)
        return true;
    }

    private List<TimeSlotResponse> createTimeSlots(ProductResponseDto product, OperatingHours dayHours, LocalDate date) {
        List<TimeSlotResponse> slots = new ArrayList<>();

        LocalTime startTime = dayHours.getStartTime();
        LocalTime endTime = dayHours.getEndTime();
        int durationMin = product.getDurationMin() != null ? product.getDurationMin() : 60;

        // 24시간 운영인 경우 합리적인 시간대로 제한
        if (startTime.equals(LocalTime.of(0, 0)) && endTime.equals(LocalTime.of(23, 59))) {
            log.info("24시간 운영 감지, 운영시간을 09:00-21:00으로 조정");
            startTime = LocalTime.of(9, 0);
            endTime = LocalTime.of(21, 0);
        }

        log.info("시간 슬롯 생성 파라미터 - 시작: {}, 종료: {}, 지속시간: {}분", startTime, endTime, durationMin);

        LocalTime currentTime = startTime;
        int slotCount = 0;

        while (currentTime.plusMinutes(durationMin).isBefore(endTime) ||
                currentTime.plusMinutes(durationMin).equals(endTime)) {

            // 안전장치: 너무 많은 슬롯 생성 방지
            if (slotCount >= 100) {
                log.warn("시간 슬롯이 {}개를 초과하여 생성 중단", slotCount);
                break;
            }
            LocalTime slotEndTime = currentTime.plusMinutes(durationMin);

            // 과거 슬롯 제외
            LocalDateTime slotDateTime = LocalDateTime.of(date, currentTime);
            if(slotDateTime.isBefore(LocalDateTime.now())) {
                log.debug("과거 슬롯 제외: {}", slotDateTime);
                currentTime = currentTime.plusMinutes(durationMin);
                continue;
            }

            // 예약 현황 체크 (임시로 비활성화)
            int currentBookings = 0; // 무조건 0으로 설정 (모든 시간 예약 가능)

            boolean isAvailable = currentBookings == 0;

            slots.add(TimeSlotResponse.builder()
                    .startTime(currentTime)
                    .endTime(slotEndTime)
                    .isAvailable(isAvailable)
                    .maxBookings(1)
                    .price(product.getPrice())
                    .isAllDay(false)
                    .build());

            slotCount++;
            log.debug("슬롯 {}개 생성됨: {}-{}", slotCount, currentTime, slotEndTime);
            currentTime = currentTime.plusMinutes(durationMin);
        }

        log.info("총 {}개의 시간 슬롯 생성 완료", slots.size());
        return slots;
    }

    private List<TimeSlotResponse> createAllDaySlot(ProductResponseDto product, OperatingHours dayHours, LocalDate date) {

        // 과거날짜 예약 불가
        if(date.isBefore(LocalDate.now())) {
            return Collections.emptyList();
        }

        int currentBookings = 0; // 임시로 비활성화

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
