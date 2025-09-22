// src/main/java/com/petmate/domain/review/entity/ReviewEntity.java
package com.petmate.domain.review.entity;

import com.petmate.common.entity.BaseEntity;
import com.petmate.domain.booking.entity.BookingEntity;
import com.petmate.domain.company.entity.CompanyEntity;
import com.petmate.domain.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // 리뷰 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private BookingEntity reservation; // 예약 (BookingEntity)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserEntity ownerUser; // 리뷰 작성자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private CompanyEntity company; // 업체

    @Column(nullable = false)
    private Integer rating; // 별점 (1~5)

    @Column(columnDefinition = "TEXT")
    private String comment; // 코멘트

    // ReviewEntity.java
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible;   // true/false 저장
}
