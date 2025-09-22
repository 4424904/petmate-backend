// src/main/java/com/petmate/domain/review/repository/jpa/ReviewRepository.java
package com.petmate.domain.review.repository.jpa;

import com.petmate.domain.review.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Integer> {

    // 예약 ID로 리뷰 존재 여부 확인
    boolean existsByReservation_Id(Integer reservationId);

    // 예약 ID + 작성자 ID로 단건 조회
    Optional<ReviewEntity> findByReservation_IdAndOwnerUser_Id(Integer reservationId, Integer ownerUserId);

    // 작성자 기준 최신순 조회
    List<ReviewEntity> findByOwnerUser_IdOrderByCreatedAtDesc(Integer ownerUserId, Pageable pageable);

    // 업체 기준 페이지네이션 조회
    Page<ReviewEntity> findByCompany_Id(Integer companyId, Pageable pageable);

    // 작성자 + 업체 기준 최신순 조회
    List<ReviewEntity> findByOwnerUser_IdAndCompany_IdOrderByCreatedAtDesc(Integer ownerUserId, Integer companyId, Pageable pageable);
}
