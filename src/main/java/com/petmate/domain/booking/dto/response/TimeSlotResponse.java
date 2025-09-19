package com.petmate.domain.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotResponse {

    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isAvailable;
    private int currentBookings;
    private int maxBookings;
    private Integer price;
    private boolean isAllDay;

}
