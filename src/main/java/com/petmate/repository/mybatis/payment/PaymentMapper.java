package com.petmate.repository.mybatis.payment;

import com.petmate.domain.payment.entity.PaymentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PaymentMapper {

    PaymentEntity findById(@Param("id") int id);

    List<PaymentEntity> findByReservationId(@Param("reservationId") int reservationId);

    int updateStatus(@Param("id") int id, @Param("status") int status);
}
