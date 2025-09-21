// review/repository/jpa/ReviewKeywordRepository.java
package com.petmate.domain.review.repository.jpa;

import com.petmate.domain.review.entity.ReviewKeywordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewKeywordRepository extends JpaRepository<ReviewKeywordEntity, Integer> {
    List<ReviewKeywordEntity> findByServiceTypeOrderByIdAsc(String serviceType);
    List<ReviewKeywordEntity> findByServiceTypeAndIsActiveOrderByIdAsc(String serviceType, Integer isActive);

}
