package com.petmate.domain.product.entity;

import com.petmate.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")    // 상품 아이디
    private Integer id;

    @Column(name = "company_id", nullable = false)     // 업체
    private Integer companyId;

    @Column(name = "service_type", nullable = false, length = 1)    // 서비스
    private String serviceType;

    @Column(name = "name", nullable = false, length = 150)  // 상품명
    private String name;

    @Column(name = "price", nullable = false)   // 가격
    private Integer price;

    @Column(name = "all_day", nullable = false) // 종일
    @Builder.Default
    private Integer allDay = 0;

    @Column(name = "duration_min")  // 소요시간(분)
    private Integer durationMin;

    @Column(name = "intro_text")    // 소개
    private String introText;

    @Column(name = "min_pet", nullable = false)   // 최소 마리수
    @Builder.Default
    private Integer minPet = 1;

    @Column(name = "max_pet", nullable = false) // 최대 마리수
    @Builder.Default
    private Integer maxPet = 1;

    @Column(name = "is_active", nullable = false)   // 사용 여부
    @Builder.Default
    private Integer isActive = 1;






}
