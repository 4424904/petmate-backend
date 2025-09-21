// src/main/java/com/petmate/domain/review/entity/ReviewKeywordEntity.java
package com.petmate.domain.review.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "review_keyword")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewKeywordEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name="service_type", nullable=false, length=40)
    private String serviceType;

    @Column(nullable=false, length=80)
    private String label;

    @Column(nullable=false, length=40)
    private String category;

    @Column(name="weight", nullable=false, precision=4, scale=2)
    private BigDecimal weight;        // ✅ DECIMAL → BigDecimal

    @Column(name="is_positive", nullable=false)
    private Integer isPositive;       // tinyint

    @Column(name="is_active", nullable=false)
    private Integer isActive;         // tinyint
}
