// src/main/java/com/petmate/domain/review/dto/request/ReviewRequestDto.java
package com.petmate.domain.review.dto.request;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewRequestDto {
    private Integer reservationId;
    private Integer companyId;
    private Integer rating;
    private String comment;
    private List<Integer> keywordIds; // ← Long → Integer 로 수정
}
