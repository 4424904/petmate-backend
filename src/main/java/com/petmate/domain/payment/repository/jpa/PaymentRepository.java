package com.petmate.domain.payment.repository.jpa;


import com.petmate.domain.payment.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Integer> {

    List<PaymentEntity> findByReservationId(int reservationId);

    Optional<PaymentEntity> findByProviderTxId(String providerTxId);

    List<PaymentEntity> findByStatus(String status);

}
