// review/service/ReviewKeywordService.java
package com.petmate.domain.review.service;

import com.petmate.domain.review.entity.ReviewKeywordEntity;
import com.petmate.domain.review.repository.jpa.ReviewKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewKeywordService {
    private final ReviewKeywordRepository repo;

    public List<ReviewKeywordEntity> list(String serviceType, Integer activeOnly) {
        if (serviceType == null || serviceType.isBlank()) {
            throw new IllegalArgumentException("serviceType 필요");
        }

        if (activeOnly != null) {
            return repo.findByServiceTypeAndIsActiveOrderByIdAsc(serviceType, activeOnly);
        } else {
            return repo.findByServiceTypeOrderByIdAsc(serviceType);
        }
    }

}
