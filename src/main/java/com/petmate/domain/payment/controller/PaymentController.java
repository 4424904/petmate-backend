package com.petmate.domain.payment.controller;

import com.petmate.domain.payment.dto.request.PaymentRequestDto;
import com.petmate.domain.payment.dto.response.PaymentResponseDto;
import com.petmate.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
//@CrossOrigin(origins = "*")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.front-base-url}")
    private String frontendUrl;

    @PostMapping("/process")
    public ResponseEntity<PaymentResponseDto> processPayment(@RequestBody PaymentRequestDto request) {
        log.info("=== 결제 요청 수신 ===");
        log.info("요청 헤더 확인 완료");
        log.info("받은 요청 데이터: {}", request);

        try {
            PaymentResponseDto response = paymentService.processPayment(request);
            
            log.info("=== 서비스 처리 완료 ===");
            log.info("응답 성공 여부: {}", response.isSuccess());

            if (response.isSuccess()) {
                log.info("결제 성공 응답 반환");
                return ResponseEntity.ok(response);
            } else {
                log.warn("결제 실패 응답 반환: {}", response.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            log.error("=== 컨트롤러에서 예외 발생 ===", e);
            PaymentResponseDto errorResponse = PaymentResponseDto.failure("서버 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponseDto> getPayment(@PathVariable int paymentId) {
        log.info("Get payment request: {}", paymentId);

        return paymentService.getPayment(paymentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reservation/{reservationId}")
    public ResponseEntity<List<PaymentResponseDto>> getPaymentsByReservation(@PathVariable int reservationId) {
        log.info("Get payments by reservation: {}", reservationId);

        List<PaymentResponseDto> payments = paymentService.getPaymentsByReservation(reservationId);
        return ResponseEntity.ok(payments);
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResponseDto> cancelPayment(@PathVariable int paymentId) {
        log.info("Cancel payment request: {}", paymentId);

        try {
            PaymentResponseDto response = paymentService.cancelPayment(paymentId);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (Exception e) {
            log.error("Payment cancellation error: ", e);
            PaymentResponseDto errorResponse = PaymentResponseDto.failure("결제 취소 중 오류가 발생했습니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Payment service is running");
    }

    // 결제 상태 조회 API
    @GetMapping("/status/{orderId}")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String orderId) {
        log.info("결제 상태 조회 요청: {}", orderId);

        try {
            PaymentResponseDto payment = paymentService.getPaymentByOrderId(orderId);

            Map<String, Object> response = new HashMap<>();
            if (payment != null) {
                response.put("status", payment.getStatus());
                response.put("orderId", orderId);
                response.put("amount", payment.getAmount());
                response.put("transactionId", payment.getProviderTxId());
            } else {
                response.put("status", "NOT_FOUND");
                response.put("orderId", orderId);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("결제 상태 조회 실패: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("orderId", orderId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // 다날 결제 성공 콜백 처리
    @GetMapping("/danal/success")
    public void handleDanalSuccess(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String status,
            HttpServletResponse response
    ) throws Exception {
        log.info("=== 다날 결제 성공 콜백 수신 ===");
        log.info("orderId: {}, transactionId: {}, amount: {}, status: {}", orderId, transactionId, amount, status);

        try {
            // 결제 성공 처리 로직
            if (orderId != null) {
                paymentService.handlePaymentSuccess(orderId, transactionId, amount);
            }

            // 프론트엔드 성공 페이지로 리다이렉트
            String frontendSuccessUrl = frontendUrl + "/payment/success" +
                    "?orderId=" + (orderId != null ? orderId : "") +
                    "&transactionId=" + (transactionId != null ? transactionId : "") +
                    "&amount=" + (amount != null ? amount : "");
            
            response.sendRedirect(frontendSuccessUrl);

        } catch (Exception e) {
            log.error("결제 성공 처리 중 오류 발생", e);
            response.sendRedirect(frontendUrl + "/payment/fail?error=processing_error");
        }
    }

    // 다날 결제 실패 콜백 처리
    @GetMapping("/danal/fail")
    public void handleDanalFail(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String errorMessage,
            HttpServletResponse response
    ) throws Exception {
        log.info("=== 다날 결제 실패 콜백 수신 ===");
        log.info("orderId: {}, errorCode: {}, errorMessage: {}", orderId, errorCode, errorMessage);

        try {
            // 결제 실패 처리 로직
            if (orderId != null) {
                paymentService.handlePaymentFailure(orderId, errorCode, errorMessage);
            }

            // 프론트엔드 실패 페이지로 리다이렉트
            String frontendFailUrl = frontendUrl + "/payment/fail" +
                    "?orderId=" + (orderId != null ? orderId : "") +
                    "&errorCode=" + (errorCode != null ? errorCode : "") +
                    "&errorMessage=" + (errorMessage != null ? errorMessage : "");
            
            response.sendRedirect(frontendFailUrl);

        } catch (Exception e) {
            log.error("결제 실패 처리 중 오류 발생", e);
            response.sendRedirect(frontendUrl + "/payment/fail?error=processing_error");
        }
    }

    // POST 방식 콜백도 지원 (다날에서 POST로 콜백할 수도 있음)
    @PostMapping("/danal/success")
    public void handleDanalSuccessPost(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String status,
            HttpServletResponse response
    ) throws Exception {
        handleDanalSuccess(orderId, transactionId, amount, status, response);
    }

    @PostMapping("/danal/fail")
    public void handleDanalFailPost(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String errorCode,
            @RequestParam(required = false) String errorMessage,
            HttpServletResponse response
    ) throws Exception {
        handleDanalFail(orderId, errorCode, errorMessage, response);
    }
}
