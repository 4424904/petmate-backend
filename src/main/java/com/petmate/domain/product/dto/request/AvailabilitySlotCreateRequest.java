package com.petmate.domain.product.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AvailabilitySlotCreateRequest {

    @NotNull
    private Integer companyId;

    @NotNull
    private Integer productId;

    @NotNull
    private LocalDate slotDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @NotNull
    private Integer capacity;
}
