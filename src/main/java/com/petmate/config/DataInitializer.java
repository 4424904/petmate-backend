package com.petmate.config;

import com.petmate.payment.entity.GroupCodeEntity;
import com.petmate.payment.repository.jpa.CommonCodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CommonCodeRepository commonCodeRepository;

    @Override
    public void run(String... args) throws Exception {
        initializeCommonCodes();
    }

    private void initializeCommonCodes() {
        // 이미 데이터가 있으면 스킵
        if (commonCodeRepository.count() > 0) {
            log.info("Common codes already initialized");
            return;
        }

        log.info("Initializing common codes...");

        // 결제 상태 코드
        List<GroupCodeEntity> paymentStatusCodes = Arrays.asList(
            GroupCodeEntity.builder()
                .codeType("PAYMENT_STATUS")
                .codeValue("0")
                .codeName("PENDING")
                .codeDesc("결제대기")
                .sortOrder(1)
                .useYn("Y")
                .build(),
            GroupCodeEntity.builder()
                .codeType("PAYMENT_STATUS")
                .codeValue("1")
                .codeName("PAID")
                .codeDesc("결제완료")
                .sortOrder(2)
                .useYn("Y")
                .build(),
            GroupCodeEntity.builder()
                .codeType("PAYMENT_STATUS")
                .codeValue("2")
                .codeName("CANCELLED")
                .codeDesc("결제취소")
                .sortOrder(3)
                .useYn("Y")
                .build(),
            GroupCodeEntity.builder()
                .codeType("PAYMENT_STATUS")
                .codeValue("3")
                .codeName("FAILED")
                .codeDesc("결제실패")
                .sortOrder(4)
                .useYn("Y")
                .build()
        );

        // 결제사 코드
        List<GroupCodeEntity> paymentProviderCodes = Arrays.asList(
            GroupCodeEntity.builder()
                .codeType("PAYMENT_PROVIDER")
                .codeValue("DANAL")
                .codeName("DANAL")
                .codeDesc("다날페이")
                .sortOrder(1)
                .useYn("Y")
                .build(),
            GroupCodeEntity.builder()
                .codeType("PAYMENT_PROVIDER")
                .codeValue("TOSS")
                .codeName("TOSS")
                .codeDesc("토스페이")
                .sortOrder(2)
                .useYn("Y")
                .build(),
            GroupCodeEntity.builder()
                .codeType("PAYMENT_PROVIDER")
                .codeValue("KAKAO")
                .codeName("KAKAO")
                .codeDesc("카카오페이")
                .sortOrder(3)
                .useYn("Y")
                .build(),
            GroupCodeEntity.builder()
                .codeType("PAYMENT_PROVIDER")
                .codeValue("NAVER")
                .codeName("NAVER")
                .codeDesc("네이버페이")
                .sortOrder(4)
                .useYn("Y")
                .build(),
            GroupCodeEntity.builder()
                .codeType("PAYMENT_PROVIDER")
                .codeValue("CARD")
                .codeName("CARD")
                .codeDesc("신용카드")
                .sortOrder(5)
                .useYn("Y")
                .build(),
            GroupCodeEntity.builder()
                .codeType("PAYMENT_PROVIDER")
                .codeValue("BANK")
                .codeName("BANK")
                .codeDesc("계좌이체")
                .sortOrder(6)
                .useYn("Y")
                .build()
        );

        // 데이터 저장
        commonCodeRepository.saveAll(paymentStatusCodes);
        commonCodeRepository.saveAll(paymentProviderCodes);

        log.info("Common codes initialized successfully");
    }
}