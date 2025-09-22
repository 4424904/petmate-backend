// src/main/java/com/petmate/domain/review/dto/response/ReviewResponseDto.java
package com.petmate.domain.review.dto.response;

import com.petmate.domain.review.entity.ReviewEntity;
<<<<<<< HEAD
import com.petmate.domain.user.entity.UserEntity;
=======
>>>>>>> ea1543b5233146b9006eb2d1e3dd05ad78f90955
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
<<<<<<< HEAD

=======
// ReviewResponseDto.java
>>>>>>> ea1543b5233146b9006eb2d1e3dd05ad78f90955
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponseDto {
<<<<<<< HEAD

    private Integer id;
    private Integer reservationId;
    private Long ownerUserId;
    private Integer companyId;

    private String ownerNickName;   // ✅ 닉네임만 따로 전달
    private String ownerName;       // 이름/닉네임 표시용
    private String ownerMaskedName; // 필요 없으면 제거해도 됨

    private Integer rating;
    private String comment;
    private Boolean isVisible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

=======
    private Integer id;
    private Integer reservationId;   // BookingEntity.id (현재 Integer)
    private Long    ownerUserId;     // ← Long로 변경
    private Integer companyId;

    private Integer rating;
    private String  comment;
    private Boolean isVisible;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
>>>>>>> ea1543b5233146b9006eb2d1e3dd05ad78f90955
    private List<KeywordDto> keywords;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class KeywordDto {
        private Integer id;
        private String label;
        private String category;
        private String serviceType;
    }

    public static ReviewResponseDto from(ReviewEntity e, List<KeywordDto> kws) {
<<<<<<< HEAD
        String nick = e.getOwnerUser() != null ? e.getOwnerUser().getNickName() : null;
        String display = extractDisplayName(e.getOwnerUser());

        return ReviewResponseDto.builder()
                .id(e.getId())
                .reservationId(e.getReservation() != null ? e.getReservation().getId() : null)
                .ownerUserId(e.getOwnerUser() != null ? e.getOwnerUser().getId() : null)
                .companyId(e.getCompany() != null ? e.getCompany().getId() : null)
                .ownerNickName(nick)   // ✅ 닉네임 세팅
                .ownerName(display)    // fallback 용
                .ownerMaskedName(display) // 필요 없다면 제거
=======
        return ReviewResponseDto.builder()
                .id(e.getId())
                .reservationId(e.getReservation() != null ? e.getReservation().getId() : null)
                .ownerUserId(e.getOwnerUser() != null ? e.getOwnerUser().getId() : null) // Long OK
                .companyId(e.getCompany() != null ? e.getCompany().getId() : null)
>>>>>>> ea1543b5233146b9006eb2d1e3dd05ad78f90955
                .rating(e.getRating())
                .comment(e.getComment())
                .isVisible(e.getIsVisible())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .keywords(kws)
                .build();
    }
<<<<<<< HEAD

    private static String extractDisplayName(UserEntity u) {
        if (u == null) return null;
        if (u.getNickName() != null && !u.getNickName().isBlank()) return u.getNickName();
        if (u.getName() != null && !u.getName().isBlank()) return u.getName();
        if (u.getEmail() != null && !u.getEmail().isBlank()) return u.getEmail().split("@")[0];
        return null;
    }
=======
>>>>>>> ea1543b5233146b9006eb2d1e3dd05ad78f90955
}
