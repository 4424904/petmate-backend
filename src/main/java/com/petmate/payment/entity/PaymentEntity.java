package com.petmate.payment.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "reservation_id", nullable = false)
    private int reservationId;

    @Column(name = "provider", length = 20, nullable = false)
    private String provider;

    @Column(name = "provider_tx_id", length = 100)
    private String providerTxId;

    @Column(nullable = false)
    private int amount;

    @Builder.Default
    @Column(length = 3, nullable = false)
    private String currency = "KRW";

    @Builder.Default
    @Column(length = 1, nullable = false)
    private String status = "0";

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "raw_json", columnDefinition = "JSON")
    private String rawJson;

}
