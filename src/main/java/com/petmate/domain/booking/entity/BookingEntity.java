package com.petmate.domain.booking.entity;

import com.petmate.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "OWNER_USER_ID", nullable = false)
    private Integer ownerUserId;

    @Column(name = "COMPANY_ID", nullable = false)
    private Integer companyId;

    @Column(name = "PRODUCT_ID", nullable = false)
    private Integer productId;

    @Column(name = "STATUS", nullable = false, length = 1)
    @Builder.Default
    private String status = "0";

    @Column(name = "START_DT", nullable = false)
    private LocalDateTime startDt;

    @Column(name = "END_DT", nullable = false)
    private LocalDateTime endDt;

    @Column(name = "PET_COUNT", nullable = false)
    private Integer petCount;

    @Column(name = "SPECIAL_REQUEST", columnDefinition = "TEXT")
    private String specialRequest;

    @Column(name = "TOTAL_PRICE", nullable = false)
    private Integer totalPrice;

    @Column(name = "PAYMENT_STATUS", nullable = false, length = 1)
    @Builder.Default
    private String paymentStatus = "0";


}
