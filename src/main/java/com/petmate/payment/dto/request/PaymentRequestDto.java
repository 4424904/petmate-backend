package com.petmate.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    private int reservationId;

    private String provider;

    private int amount;

    private String currency = "KRW";

    private String paymentMethod;

    private String customerName;

    private String customerEmail;

    private String customerPhone;

}
