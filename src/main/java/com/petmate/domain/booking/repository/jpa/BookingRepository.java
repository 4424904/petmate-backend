// src/main/java/com/petmate/domain/booking/repository/jpa/BookingRepository.java
package com.petmate.domain.booking.repository.jpa;

import com.petmate.domain.booking.entity.BookingEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<BookingEntity, Integer> {

    /** 특정 사용자 예약 목록 (최신순) */
    List<BookingEntity> findByOwnerUserIdOrderByCreatedAtDesc(Integer ownerUserId, Pageable pageable);

    /** 특정 사용자 + 업체 예약 목록 (최신순) */
    List<BookingEntity> findByOwnerUserIdAndCompanyIdOrderByCreatedAtDesc(Integer ownerUserId, Integer companyId, Pageable pageable);

    /** 특정 업체 예약 목록 */
    List<BookingEntity> findByCompanyId(Integer companyId, Pageable pageable);

    /** 특정 상품 + 시간 범위 예약 조회 */
    List<BookingEntity> findByProductIdAndStartDtLessThanEqualAndEndDtGreaterThanEqualAndStatusNotIn(
            Integer productId,
            LocalDateTime end,
            LocalDateTime start,
            List<String> excludeStatus
    );

    /** 특정 날짜의 종일 예약 수 (ALL_DAY 같은 제약 대신 예약 테이블만 확인하는 경우) */
    long countByProductIdAndStartDtBetweenAndStatusNotIn(
            Integer productId,
            LocalDateTime dayStart,
            LocalDateTime dayEnd,
            List<String> excludeStatus
    );
}
