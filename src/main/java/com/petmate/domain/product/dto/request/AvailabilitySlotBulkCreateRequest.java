package com.petmate.domain.product.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class AvailabilitySlotBulkCreateRequest {

    @NotNull
    private Integer companyId;

    @NotNull
    private Integer productId;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @NotNull
    private List<TimeSlot> timeSlots; // 반복할 시간대들

    @NotNull
    private Integer capacity;

    @Data
    public static class TimeSlot {
        private LocalTime startTime;
        private LocalTime endTime;
    }

}
