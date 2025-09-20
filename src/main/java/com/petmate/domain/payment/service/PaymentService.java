package com.petmate.domain.payment.service;

import com.petmate.domain.payment.dto.request.PaymentRequestDto;
import com.petmate.domain.payment.dto.response.PaymentResponseDto;
import com.petmate.domain.payment.entity.GroupCodeEntity;
import com.petmate.domain.payment.entity.PaymentEntity;
import com.petmate.domain.payment.repository.jpa.CommonCodeRepository;
import com.petmate.domain.payment.repository.jpa.PaymentRepository;
import com.petmate.domain.booking.repository.mybatis.BookingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final BookingMapper bookingMapper;

    public PaymentResponseDto processPayment(PaymentRequestDto reqDto) {
        try {
            log.info("=== 결제 처리 시작 ===");
            log.info("요청 데이터: {}", reqDto);
            log.info("예약 ID: {}, 결제사: {}, 금액: {}, 결제수단: {}", 
                reqDto.getReservationId(), reqDto.getProvider(), reqDto.getAmount(), reqDto.getPaymentMethod());

            String mockTxId = generateMockTxId(reqDto.getProvider());
            log.info("생성된 거래 ID: {}", mockTxId);

            PaymentEntity paymentEntity = PaymentEntity.builder()
                    .reservationId(reqDto.getReservationId())
                    .provider(reqDto.getProvider())
                    .providerTxId(mockTxId)
                    .amount(reqDto.getAmount())
                    .currency(reqDto.getCurrency() != null ? reqDto.getCurrency() : "KRW")
                    .status("1")
                    .paidAt(LocalDateTime.now())
                    .rawJson(createMockResponse(reqDto, mockTxId))
                    .build();

            log.info("저장할 Entity: {}", paymentEntity);

            PaymentEntity savedPayment = paymentRepository.save(paymentEntity);

            log.info("저장 완료 - 결제 ID: {}, 상태: {}", savedPayment.getId(), savedPayment.getStatus());

            // 🔥 결제 성공 시 reservation 테이블의 payment_status도 업데이트
            if ("1".equals(savedPayment.getStatus())) {
                log.info("결제 성공 - reservation ID {}의 payment_status를 1로 업데이트", reqDto.getReservationId());
                bookingMapper.updatePaymentStatus(reqDto.getReservationId(), "1");
            }

            String statusDesc = getStatusDescription(savedPayment.getStatus());
            String providerDesc = getProviderDescription(savedPayment.getProvider());

            PaymentResponseDto response = PaymentResponseDto.success(savedPayment, statusDesc, providerDesc);
            log.info("응답 데이터: {}", response);

            return response;
        } catch (Exception e) {
            log.error("=== 결제 처리 실패 ===", e);
            log.error("에러 타입: {}", e.getClass().getSimpleName());
            log.error("에러 메시지: {}", e.getMessage());
            return PaymentResponseDto.failure("결제 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Optional<PaymentResponseDto> getPayment(int paymentId) {
        return paymentRepository.findById(paymentId)
                .map(paymentEntity -> {
                   String statusDesc = getStatusDescription(paymentEntity.getStatus());
                   String providerDesc = getProviderDescription(paymentEntity.getProvider());
                   return PaymentResponseDto.from(paymentEntity, statusDesc, providerDesc);
                });
    }

    @Transactional(readOnly = true)
    public List<PaymentResponseDto> getPaymentsByReservation(int reservationId) {
        return paymentRepository.findByReservationId(reservationId)
                .stream()
                .map(paymentEntity -> {
                    String statusDesc = getStatusDescription(paymentEntity.getStatus());
                    String providerDesc = getProviderDescription(paymentEntity.getProvider());
                    return PaymentResponseDto.from(paymentEntity, statusDesc, providerDesc);
                })
                .collect(Collectors.toList());
    }

    public PaymentResponseDto cancelPayment(int paymentId) {
        try {
            Optional<PaymentEntity> paymentOpt = paymentRepository.findById(paymentId);

            if(paymentOpt.isEmpty()) {
                return PaymentResponseDto.failure("결제 정보를 찾을 수 없습니다.");
            }

            PaymentEntity paymentEntity = paymentOpt.get();
            
            log.info("취소 전 상태: {}", paymentEntity.getStatus());

            if(!"1".equals(paymentEntity.getStatus())) {
                return PaymentResponseDto.failure("취소할 수 없는 결제 상태입니다. 현재 상태: " + paymentEntity.getStatus());
            }

            // 상태 변경
            paymentEntity.setStatus("2");
            paymentEntity.setCancelledAt(LocalDateTime.now());
            paymentEntity.setRawJson(paymentEntity.getRawJson() +
                    String.format(",\"cancelled_at\":\"%s\",\"cancel_reason\":\"사용자 요청\"",
                            LocalDateTime.now()));

            // 명시적으로 저장하고 플러시
            PaymentEntity cancelledPayment = paymentRepository.saveAndFlush(paymentEntity);
            
            log.info("취소 후 상태: {}", cancelledPayment.getStatus());

            String statusDesc = getStatusDescription(cancelledPayment.getStatus());
            String providerDesc = getProviderDescription(cancelledPayment.getProvider());

            PaymentResponseDto response = PaymentResponseDto.from(cancelledPayment, statusDesc, providerDesc);
            response.setMessage("결제가 성공적으로 취소되었습니다.");

            return response;
        } catch (Exception e) {
            log.error("Payment cancellation failed: ", e);
            return PaymentResponseDto.failure("결제 취소 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    private String generateMockTxId(String provider) {
        String prefix = "DANAL".equals(provider) ? "DN" : provider.toUpperCase();
        return prefix + "_" + System.currentTimeMillis() + "_" +
                String.format("%04d", (int)(Math.random() * 10000));
    }

    private String createMockResponse(PaymentRequestDto request, String txId) {
        return String.format(
                "{\"provider_tx_id\":\"%s\",\"amount\":%d,\"currency\":\"%s\",\"status\":\"SUCCESS\"," +
                        "\"payment_method\":\"%s\",\"paid_at\":\"%s\",\"customer_name\":\"%s\"," +
                        "\"provider\":\"%s\",\"order_id\":\"%s\"}",
                txId,
                request.getAmount(),
                request.getCurrency(),
                request.getPaymentMethod() != null ? request.getPaymentMethod() : "INTEGRATED",
                LocalDateTime.now(),
                request.getCustomerName() != null ? request.getCustomerName() : "고객",
                request.getProvider(),
                "ORDER_" + System.currentTimeMillis()
        );
    }

    private String getStatusDescription(String statusCode) {
        return commonCodeRepository.findByCodeTypeAndCodeValue("PAYMENT_STATUS", statusCode)
                .map(GroupCodeEntity::getCodeDesc)
                .orElse("알 수 없는 상태");
    }

    private String getProviderDescription(String providerCode) {
        return commonCodeRepository.findByCodeTypeAndCodeValue("PAYMENT_PROVIDER", providerCode)
                .map(GroupCodeEntity::getCodeDesc)
                .orElse("알 수 없는 결제사");
    }

    // 다날 결제 성공 처리
    public void handlePaymentSuccess(String orderId, String transactionId, String amount) {
        try {
            log.info("=== 결제 성공 처리 시작 ===");
            log.info("orderId: {}, transactionId: {}, amount: {}", orderId, transactionId, amount);

            // orderId로 결제 정보 조회 (raw_json에서 order_id로 검색)
            List<PaymentEntity> payments = paymentRepository.findAll().stream()
                    .filter(p -> p.getRawJson() != null && p.getRawJson().contains("\"order_id\":\"" + orderId + "\""))
                    .toList();

            if (!payments.isEmpty()) {
                PaymentEntity payment = payments.get(0);
                
                // 결제 상태를 성공으로 업데이트
                payment.setStatus("1"); // 성공 상태
                payment.setProviderTxId(transactionId);
                payment.setPaidAt(LocalDateTime.now());
                
                // raw_json 업데이트
                String updatedJson = payment.getRawJson().replace("\"status\":\"SUCCESS\"", 
                    "\"status\":\"SUCCESS\",\"confirmed_at\":\"" + LocalDateTime.now() + "\",\"danal_tx_id\":\"" + transactionId + "\"");
                payment.setRawJson(updatedJson);
                
                paymentRepository.save(payment);

                // reservation 테이블의 payment_status도 업데이트
                log.info("결제 성공 - reservation ID {}의 payment_status를 1로 업데이트", payment.getReservationId());
                bookingMapper.updatePaymentStatus(payment.getReservationId(), "1");

                log.info("결제 성공 처리 완료 - Payment ID: {}", payment.getId());
            } else {
                log.warn("결제 정보를 찾을 수 없음 - orderId: {}", orderId);
            }
            
        } catch (Exception e) {
            log.error("결제 성공 처리 중 오류 발생", e);
            throw new RuntimeException("결제 성공 처리 실패", e);
        }
    }

    // 다날 결제 실패 처리
    public void handlePaymentFailure(String orderId, String errorCode, String errorMessage) {
        try {
            log.info("=== 결제 실패 처리 시작 ===");
            log.info("orderId: {}, errorCode: {}, errorMessage: {}", orderId, errorCode, errorMessage);

            // orderId로 결제 정보 조회
            List<PaymentEntity> payments = paymentRepository.findAll().stream()
                    .filter(p -> p.getRawJson() != null && p.getRawJson().contains("\"order_id\":\"" + orderId + "\""))
                    .toList();

            if (!payments.isEmpty()) {
                PaymentEntity payment = payments.get(0);
                
                // 결제 상태를 실패로 업데이트
                payment.setStatus("3"); // 실패 상태
                payment.setCancelledAt(LocalDateTime.now());
                
                // raw_json 업데이트
                String updatedJson = payment.getRawJson().replace("\"status\":\"SUCCESS\"", 
                    "\"status\":\"FAILED\",\"failed_at\":\"" + LocalDateTime.now() + 
                    "\",\"error_code\":\"" + errorCode + "\",\"error_message\":\"" + errorMessage + "\"");
                payment.setRawJson(updatedJson);
                
                paymentRepository.save(payment);
                
                log.info("결제 실패 처리 완료 - Payment ID: {}", payment.getId());
            } else {
                log.warn("결제 정보를 찾을 수 없음 - orderId: {}", orderId);
            }
            
        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류 발생", e);
            throw new RuntimeException("결제 실패 처리 실패", e);
        }
    }

    public PaymentResponseDto getPaymentByOrderId(String orderId) {
        log.info("OrderId로 결제 조회 : {}" , orderId);

        try {
            if (orderId.startsWith("booking_")) {
                String[] parts = orderId.split("_");
                if (parts.length >= 2) {
                    try {
                        int reservationId = Integer.parseInt(parts[1]);
                        List<PaymentEntity> payments =
                                paymentRepository.findByReservationId(reservationId);

                        if (!payments.isEmpty()) {
                            // 가장 최근 결제 반환
                            PaymentEntity latestPayment = payments.get(payments.size() -
                                    1);
                            return PaymentResponseDto.from(latestPayment);
                        }
                    } catch (NumberFormatException e) {
                        log.warn("OrderId에서 reservationId 추출 실패: {}", orderId);
                    }
                }
            }

            // providerTxId로 직접 검색(제발..)
            Optional<PaymentEntity> paymentOpt =
                    paymentRepository.findByProviderTxId(orderId);
            if (paymentOpt.isPresent()) {
                return PaymentResponseDto.from(paymentOpt.get());
            }

            log.warn("OrderId로 결제 정보를 찾을 수 없음: {}", orderId);
            return null;

        } catch (Exception e) {
            log.error("OrderId로 결제 조회 중 오류: ", e);
            throw e;
        }
    }

}
