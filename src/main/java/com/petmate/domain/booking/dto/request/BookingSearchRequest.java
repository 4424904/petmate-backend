package com.petmate.domain.booking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSearchRequest {

    private String status;
    private String paymentStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer limit;
    private Integer offset;


}
