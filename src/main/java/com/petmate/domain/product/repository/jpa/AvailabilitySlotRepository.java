package com.petmate.domain.product.repository.jpa;


import com.petmate.domain.product.entity.AvailabilitySlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AvailabilitySlotRepository extends JpaRepository<AvailabilitySlotEntity, Integer> {

    // 특정 상품의 예약 가능한지 조회
    @Query("SELECT s FROM AvailabilitySlotEntity s WHERE s.productId = :productId " +
            "AND s.slotDate >= :date AND s.booked < s.capacity " +
            "ORDER BY s.slotDate, s.startDt")
    List<AvailabilitySlotEntity> findAvailableSlotsByProductId(@Param("productId") Integer productId,
                                                               @Param("date")LocalDate date);

    // 특정 날짜의 상품 조회
    //List<AvailabilitySlotEntity> findByProductIdAndSlotDateOrderByStartDt(Integer productId, LocalDate slotDate);

    // 업체 모든 상품 조회
    List<AvailabilitySlotEntity> findByCompanyIdOrderBySlotDateAscStartDt(Integer companyId);

    // 특정 상품의 모든 슬롯 삭제
    @Modifying
    void deleteByProductId(Integer productId);

    // 특정 상품의 슬롯 개수 조회
    @Query("SELECT COUNT(s) FROM AvailabilitySlotEntity s WHERE s.productId = :productId")
    Long countByProductId(@Param("productId") Integer productId);

    // 특정 상품의 예약된 슬롯 개수 조회
    @Query("SELECT COUNT(s) FROM AvailabilitySlotEntity s WHERE s.productId = :productId AND s.booked > 0")
    Long countBookedSlotsByProductId(@Param("productId") Integer productId);

}
