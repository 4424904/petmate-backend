// src/main/java/com/petmate/domain/review/dto/response/ReviewKeywordDto.java
package com.petmate.domain.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewKeywordDto {
    private Integer id;
    private String label;
    private String category;
    private String serviceType;
}
