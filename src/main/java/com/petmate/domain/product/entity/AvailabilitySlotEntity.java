package com.petmate.domain.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "availability_slot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilitySlotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "start_dt", nullable = false)
    private LocalDateTime startDt;

    @Column(name = "end_dt")
    private LocalDateTime endDt;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "booked", nullable = false)
    @Builder.Default
    private Integer booked = 0;

    // 남은 자리 계산
    public Integer getAvailableCapacity() {

        return capacity - booked;
    }

    // 예약 가능 여부
    public boolean isBookable() {

        return booked < capacity;
    }

}
