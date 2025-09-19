package com.petmate.domain.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponseDto {

    private Integer id;
    private Integer ownerUserId;
    private String ownerUserName;
    private Integer companyId;
    private String companyName;
    private Integer productId;
    private String productName;
    private Integer productPrice;
    private String status;
    private String statusName;
    private LocalDateTime startDt;
    private LocalDateTime endDt;
    private Integer petCount;
    private String specialRequest;
    private Integer totalPrice;
    private String paymentStatus;
    private String paymentStatusName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // API 응답용
    private boolean success;
    private String message;

    public static BookingResponseDto success(String message) {
        return BookingResponseDto.builder()
                .success(true)
                .message(message)
                .build();
    }

    public static  BookingResponseDto fail(String message) {
        return BookingResponseDto.builder()
                .success(false)
                .message(message)
                .build();
    }

}
