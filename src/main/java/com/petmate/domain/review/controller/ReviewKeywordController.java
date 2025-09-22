// src/main/java/com/petmate/domain/review/controller/ReviewKeywordController.java
package com.petmate.domain.review.controller;

import com.petmate.domain.review.dto.response.ReviewKeywordDto;
import com.petmate.domain.review.entity.ReviewKeywordEntity;
import com.petmate.domain.review.repository.jpa.ReviewKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/review-keywords")
@RequiredArgsConstructor
public class ReviewKeywordController {

    private final ReviewKeywordRepository repo;

    @GetMapping
    public List<ReviewKeywordDto> list(
            @RequestParam String serviceType,
            @RequestParam(required = false, defaultValue = "1") Integer activeOnly
    ) {
        List<ReviewKeywordEntity> rows = Objects.equals(activeOnly, 1)
                ? repo.findByServiceTypeAndIsActiveOrderByIdAsc(serviceType, 1)
                : repo.findByServiceTypeOrderByIdAsc(serviceType);

        return rows.stream()
                .map(k -> new ReviewKeywordDto(k.getId(), k.getLabel(), k.getCategory(), k.getServiceType()))
                .toList();
    }
}
