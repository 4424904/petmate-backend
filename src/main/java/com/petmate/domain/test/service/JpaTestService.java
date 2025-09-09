package com.petmate.domain.test.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petmate.domain.test.entity.JpaTest;
import com.petmate.domain.test.entity.JpaTestProvider;
import com.petmate.domain.test.entity.JpaTestRole;
import com.petmate.domain.test.repository.JpaTestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class JpaTestService {

    private final JpaTestRepository jpaTestRepository;

    // 전체 조회
    public List<JpaTest> findAll() {
        log.info("JPA 테스트 전체 조회");
        return jpaTestRepository.findAll();
    }

    // ID로 조회
    public JpaTest findById(Long id) {
        log.info("JPA 테스트 ID {} 조회", id);
        return jpaTestRepository.findById(id).orElse(null);
    }

    // 이메일로 조회
    public JpaTest findByEmail(String email) {
        log.info("JPA 테스트 이메일 {} 조회", email);
        return jpaTestRepository.findActiveByEmail(email).orElse(null);
    }

    // 생성
    @Transactional
    public JpaTest createTest(String name, String description) {
        log.info("JPA 테스트 생성: name={}, description={}", name, description);
        
        // 테스트용 이메일 생성 (중복 방지)
        String email = "test" + System.currentTimeMillis() + "@example.com";
        
        JpaTest jpaTest = JpaTest.builder()
                .name(name != null ? name : "테스트 이름")
                .email(email)
                .profileImage("https://via.placeholder.com/150")
                .role(JpaTestRole.GUEST)
                .provider(JpaTestProvider.KAKAO)
                .providerId("test_" + System.currentTimeMillis())
                .build();

        JpaTest savedTest = jpaTestRepository.save(jpaTest);
        log.info("JPA 테스트 생성 완료: id={}", savedTest.getId());
        return savedTest;
    }

    // 수정
    @Transactional
    public JpaTest updateTest(Long id, String name, String description) {
        log.info("JPA 테스트 수정: id={}, name={}", id, name);
        
        Optional<JpaTest> optionalTest = jpaTestRepository.findById(id);
        if (optionalTest.isPresent()) {
            JpaTest jpaTest = optionalTest.get();
            jpaTest.updateProfile(name, "https://via.placeholder.com/150");
            log.info("JPA 테스트 수정 완료: id={}", id);
            return jpaTest; // JPA dirty checking으로 자동 업데이트
        }
        
        log.warn("JPA 테스트 수정 실패 - ID {} 찾을 수 없음", id);
        return null;
    }

    // 삭제
    @Transactional
    public boolean deleteTest(Long id) {
        log.info("JPA 테스트 삭제: id={}", id);
        
        if (jpaTestRepository.existsById(id)) {
            jpaTestRepository.deleteById(id);
            log.info("JPA 테스트 삭제 완료: id={}", id);
            return true;
        }
        
        log.warn("JPA 테스트 삭제 실패 - ID {} 찾을 수 없음", id);
        return false;
    }

    // 테스트 데이터 초기화 (개발용)
    @Transactional
    public void initTestData() {
        log.info("JPA 테스트 데이터 초기화 시작");
        
        // 기존 데이터 삭제
        jpaTestRepository.deleteAll();
        
        // 샘플 데이터 생성
        for (int i = 1; i <= 5; i++) {
            JpaTest testData = JpaTest.builder()
                    .name("테스트 사용자 " + i)
                    .email("test" + i + "@example.com")
                    .profileImage("https://via.placeholder.com/150")
                    .role(i % 2 == 0 ? JpaTestRole.OWNER : JpaTestRole.PETMATE)
                    .provider(i % 3 == 0 ? JpaTestProvider.GOOGLE : 
                             i % 2 == 0 ? JpaTestProvider.NAVER : JpaTestProvider.KAKAO)
                    .providerId("init_test_" + i)
                    .build();
            
            jpaTestRepository.save(testData);
        }
        
        log.info("JPA 테스트 데이터 5개 초기화 완료");
    }

    // 통계 조회 (추가 기능)
    public long getTotalCount() {
        return jpaTestRepository.count();
    }

    // 활성 사용자 수 조회 (추가 기능)
    public long getActiveCount() {
        return jpaTestRepository.findAll().stream()
                .filter(test -> test.getIsActive())
                .count();
    }
}