package com.petmate.domain.product.dto.response;

import com.petmate.domain.product.entity.AvailabilitySlotEntity;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AvailabilitySlotResponseDto {

    private Integer id;
    private Integer companyId;
    private Integer productId;
    private String productName; // 조인으로 가져온 상품명
    private LocalDate slotDate;
    private LocalDateTime startDt;
    private LocalDateTime endDt;
    private Integer capacity;
    private Integer booked;
    private Integer availableCapacity;
    private boolean isBookable;

    public static AvailabilitySlotResponseDto from(AvailabilitySlotEntity entity) {
        return AvailabilitySlotResponseDto.builder()
                .id(entity.getId())
                .companyId(entity.getCompanyId())
                .productId(entity.getProductId())
                .slotDate(entity.getSlotDate())
                .startDt(entity.getStartDt())
                .endDt(entity.getEndDt())
                .capacity(entity.getCapacity())
                .booked(entity.getBooked())
                .availableCapacity(entity.getAvailableCapacity())
                .isBookable(entity.isBookable())
                .build();
    }
}
