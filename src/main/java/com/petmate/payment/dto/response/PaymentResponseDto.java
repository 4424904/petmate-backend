package com.petmate.payment.dto.response;

import com.petmate.payment.entity.PaymentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private int paymentId;

    private int reservationId;

    private String provider;

    private String providerTxId;

    private int amount;

    private String currency;

    private String status;

    private String statusMessage;

    private LocalDateTime paidAt;

    private LocalDateTime cancelledAt;

    private boolean success;

    private String message;

    public static PaymentResponseDto from(PaymentEntity paymentEntity) {
        return PaymentResponseDto.builder()
                .paymentId(paymentEntity.getId())
                .reservationId(paymentEntity.getReservationId())
                .provider(paymentEntity.getProvider())
                .providerTxId(paymentEntity.getProviderTxId())
                .amount(paymentEntity.getAmount())
                .currency(paymentEntity.getCurrency())
                .status(paymentEntity.getStatus())
                .statusMessage(paymentEntity.getStatus())
                .paidAt(paymentEntity.getPaidAt())
                .cancelledAt(paymentEntity.getCancelledAt())
                .success("1".equals(paymentEntity.getStatus()))
                .message("결제가 완료되었습니다.")
                .build();
    }

    public static  PaymentResponseDto success(PaymentEntity paymentEntity, String statusDesc, String providerDesc) {
        PaymentResponseDto responseDto = from(paymentEntity, statusDesc, providerDesc);
        responseDto.setSuccess(true);
        responseDto.setMessage("결제가 성공적으로 처리되었습니다.");
        return responseDto;

    }

    public static PaymentResponseDto failure(String message) {
        return PaymentResponseDto.builder()
                .success(false)
                .message(message)
                .status("3")
                .statusMessage("결제 실패")
                .build();
    }

    public static PaymentResponseDto from(PaymentEntity paymentEntity, String statusDesc, String providerDesc) {
        return PaymentResponseDto.builder()
                .paymentId(paymentEntity.getId())
                .reservationId(paymentEntity.getReservationId())
                .provider(paymentEntity.getProvider())
                .providerTxId(paymentEntity.getProviderTxId())
                .amount(paymentEntity.getAmount())
                .currency(paymentEntity.getCurrency())
                .status(paymentEntity.getStatus())
                .statusMessage(statusDesc != null ? statusDesc : "알 수 없음")
                .paidAt(paymentEntity.getPaidAt())
                .cancelledAt(paymentEntity.getCancelledAt())
                .success("1".equals(paymentEntity.getStatus()))
                .message("결제가 완료되었습니다.")
                .build();
    }


}
