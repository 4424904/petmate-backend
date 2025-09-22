package com.petmate.domain.product.service;

import com.petmate.domain.product.dto.request.AvailabilitySlotBulkCreateRequest;
import com.petmate.domain.product.dto.request.AvailabilitySlotCreateRequest;
import com.petmate.domain.product.dto.response.AvailabilitySlotResponseDto;
import com.petmate.domain.product.entity.AvailabilitySlotEntity;
import com.petmate.domain.product.repository.jpa.AvailabilitySlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilitySlotService {

    private final AvailabilitySlotRepository slotRepository;

    // 예약 가능 조회
    public List<AvailabilitySlotResponseDto> getAvailableSlots(Integer productId, LocalDate date) {
        List<AvailabilitySlotEntity> slotEntity = slotRepository.findAvailableSlotsByProductId(productId, date);
        return slotEntity.stream()
                .map(AvailabilitySlotResponseDto::from)
                .collect(Collectors.toList());
    }

    // 단일 슬롯 생성
    @Transactional
    public AvailabilitySlotResponseDto createSlot(AvailabilitySlotCreateRequest request) {
        AvailabilitySlotEntity slotEntity = AvailabilitySlotEntity.builder()
                .companyId(request.getCompanyId())
                .productId(request.getProductId())
                .startDt(LocalDateTime.of(request.getSlotDate(), request.getStartTime()))
                .endDt(LocalDateTime.of(request.getSlotDate(), request.getEndTime()))
                .capacity(request.getCapacity())
                .booked(0)
                .build();

        AvailabilitySlotEntity savedSlot = slotRepository.save(slotEntity);
        return AvailabilitySlotResponseDto.from(savedSlot);
    }

    // 대량 슬롯 생성(30분 단위 자동 생성)
    @Transactional
    public List<AvailabilitySlotResponseDto> createBulkSlots(AvailabilitySlotBulkCreateRequest request) {
        List<AvailabilitySlotEntity> slotToCreate = new ArrayList<>();

        LocalDate currentDate = request.getStartDate();
        while (!currentDate.isAfter(request.getEndDate())) {
            for (AvailabilitySlotBulkCreateRequest.TimeSlot timeSlot : request.getTimeSlots()) {
                AvailabilitySlotEntity slot = AvailabilitySlotEntity.builder()
                        .companyId(request.getCompanyId())
                        .productId(request.getProductId())
                        .slotDate(currentDate)
                        .startDt(LocalDateTime.of(currentDate, timeSlot.getStartTime()))
                        .endDt(LocalDateTime.of(currentDate, timeSlot.getEndTime()))
                        .capacity(request.getCapacity())
                        .booked(0)
                        .build();

                slotToCreate.add(slot);
            }
            currentDate = currentDate.plusDays(1);
        }
        List<AvailabilitySlotEntity> savedSlots = slotRepository.saveAll(slotToCreate);
        return savedSlots.stream()
                .map(AvailabilitySlotResponseDto::from)
                .collect(Collectors.toList());
    }

    // 업체 슬롯 조회
    public List<AvailabilitySlotResponseDto> getSlotByCompany(Integer companyId) {
        List<AvailabilitySlotEntity> slotEntity = slotRepository.findByCompanyIdOrderBySlotDateAscStartDt(companyId);
        return slotEntity.stream()
                .map(AvailabilitySlotResponseDto::from)
                .collect(Collectors.toList());
    }

    // 개별 슬롯 삭제
    @Transactional
    public void deleteSlot(Integer slotId) {
        AvailabilitySlotEntity slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("슬롯을 찾을 수 없습니다: " + slotId));

        if (slot.getBooked() > 0) {
            throw new RuntimeException("예약된 슬롯은 삭제할 수 없습니다.");
        }

        slotRepository.delete(slot);
    }

    // 상품의 모든 슬롯 삭제
    @Transactional
    public Map<String, Object> deleteSlotsByProductId(Integer productId) {
        Long totalSlots = slotRepository.countByProductId(productId);
        Long bookedSlots = slotRepository.countBookedSlotsByProductId(productId);

        if (bookedSlots > 0) {
            throw new RuntimeException("예약된 슬롯이 있어서 삭제할 수 없습니다. 예약된 슬롯: " + bookedSlots + "개");
        }

        slotRepository.deleteByProductId(productId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "모든 슬롯이 삭제되었습니다.");
        result.put("deletedCount", totalSlots);

        return result;
    }

    // 상품 슬롯 정보 조회
    public Map<String, Object> getProductSlotInfo(Integer productId) {
        Long totalSlots = slotRepository.countByProductId(productId);
        Long bookedSlots = slotRepository.countBookedSlotsByProductId(productId);

        Map<String, Object> info = new HashMap<>();
        info.put("totalSlots", totalSlots);
        info.put("bookedSlots", bookedSlots);
        info.put("availableSlots", totalSlots - bookedSlots);
        info.put("hasBookings", bookedSlots > 0);

        return info;
    }
}