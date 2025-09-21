// src/main/java/com/petmate/domain/review/dto/response/ReviewResponseDto.java
package com.petmate.domain.review.dto.response;

import com.petmate.domain.review.entity.ReviewEntity;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
// ReviewResponseDto.java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDto {
    private Integer id;
    private Integer reservationId;   // BookingEntity.id (현재 Integer)
    private Long    ownerUserId;     // ← Long로 변경
    private Integer companyId;

    private Integer rating;
    private String  comment;
    private Boolean isVisible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<KeywordDto> keywords;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class KeywordDto {
        private Integer id;
        private String label;
        private String category;
        private String serviceType;
    }

    public static ReviewResponseDto from(ReviewEntity e, List<KeywordDto> kws) {
        return ReviewResponseDto.builder()
                .id(e.getId())
                .reservationId(e.getReservation() != null ? e.getReservation().getId() : null)
                .ownerUserId(e.getOwnerUser() != null ? e.getOwnerUser().getId() : null) // Long OK
                .companyId(e.getCompany() != null ? e.getCompany().getId() : null)
                .rating(e.getRating())
                .comment(e.getComment())
                .isVisible(e.getIsVisible())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .keywords(kws)
                .build();
    }
}
