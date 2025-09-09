package com.petmate.domain.test.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petmate.domain.user.entity.UserEntity;
import com.petmate.common.repository.CodeRepository;
import com.petmate.common.util.CodeUtil;
import com.petmate.domain.test.repository.JpaTestRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class JpaTestService {

    private final JpaTestRepository jpaTestRepository;

    private final CodeUtil codeUtil;

    // 전체 조회
    public List<UserEntity> findAll() {
        log.info("JPA 테스트 전체 조회");
        List<UserEntity> userEntities = jpaTestRepository.findAll();

        userEntities.forEach(userEntity -> {
            userEntity.setRole(codeUtil.getUserRoleName(userEntity.getRole()));
            userEntity.setStatus(codeUtil.getUserStatusName(userEntity.getStatus()));
            userEntity.setMainService(codeUtil.getServiceTypeName(userEntity.getMainService()));
            userEntity.setCareSpecies(codeUtil.getSpeciesName(userEntity.getCareSpecies()));
            userEntity.setGender(codeUtil.getGenderName(userEntity.getGender()));
        });

        return userEntities;
    }

    // ID로 조회
    public UserEntity findById(Integer id) {
        log.info("JPA 테스트 ID {} 조회", id);
        return jpaTestRepository.findById(id).orElse(null);
    }

    // 이메일로 조회
    public UserEntity findByEmail(String email) {
        log.info("JPA 테스트 이메일 {} 조회", email);
        return jpaTestRepository.findActiveByEmail(email).orElse(null);
    }

    // 생성
    @Transactional
    public UserEntity createTest(String name, String description) {
        log.info("JPA 테스트 생성: name={}, description={}", name, description);
        
        // 테스트용 이메일 생성 (중복 방지)
        String email = "test" + System.currentTimeMillis() + "@example.com";
        
        UserEntity userEntity = UserEntity.builder()
                .name(name != null ? name : "테스트 이름")
                .email(email)
                .address("테스트 주소")
                .nickName("테스트닉네임" + System.currentTimeMillis())
                .provider("KAKAO")
                .profileImage("https://via.placeholder.com/150")
                .phone("010-0000-0000")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("M")
                .role("O") // 반려인
                .mainService("P")
                .careSpecies("D")
                .status("A") // 활성
                .emailVerified("N")
                .build();

        UserEntity savedTest = jpaTestRepository.save(userEntity);
        log.info("JPA 테스트 생성 완료: id={}", savedTest.getId());
        return savedTest;
    }

    // 수정
    @Transactional
    public UserEntity updateTest(Integer id, String name, String description) {
        log.info("JPA 테스트 수정: id={}, name={}", id, name);
        
        Optional<UserEntity> optionalTest = jpaTestRepository.findById(id);
        if (optionalTest.isPresent()) {
            UserEntity userEntity = optionalTest.get();
            
            // UserEntity는 불변 객체이므로 Builder 패턴으로 새 객체 생성
            UserEntity updatedEntity = UserEntity.builder()
                    .id(userEntity.getId())
                    .email(userEntity.getEmail())
                    .address(userEntity.getAddress())
                    .nickName(userEntity.getNickName())
                    .provider(userEntity.getProvider())
                    .name(name != null ? name : userEntity.getName()) // 새 이름
                    .profileImage("https://via.placeholder.com/150") // 새 프로필 이미지
                    .phone(userEntity.getPhone())
                    .birthDate(userEntity.getBirthDate())
                    .gender(userEntity.getGender())
                    .role(userEntity.getRole())
                    .mainService(userEntity.getMainService())
                    .careSpecies(userEntity.getCareSpecies())
                    .status(userEntity.getStatus())
                    .emailVerified(userEntity.getEmailVerified())
                    .build();
            
            UserEntity savedEntity = jpaTestRepository.save(updatedEntity);
            log.info("JPA 테스트 수정 완료: id={}", id);
            return savedEntity;
        }
        
        log.warn("JPA 테스트 수정 실패 - ID {} 찾을 수 없음", id);
        return null;
    }

    // 삭제
    @Transactional
    public boolean deleteTest(Integer id) {
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
            UserEntity testData = UserEntity.builder()
                    .name("테스트 사용자 " + i)
                    .email("test" + i + "@example.com")
                    .address("테스트 주소 " + i)
                    .nickName("테스트닉네임" + i)
                    .provider(i % 3 == 0 ? "GOOGLE" : 
                             i % 2 == 0 ? "NAVER" : "KAKAO")
                    .profileImage("https://via.placeholder.com/150")
                    .phone("010-000" + String.format("%d-000%d", i, i))
                    .birthDate(LocalDate.of(1990 + i, 1, 1))
                    .gender(i % 2 == 0 ? "F" : "M")
                    .role(i % 2 == 0 ? "O" : "P") // O:반려인, P:펫메이트
                    .mainService("P")
                    .careSpecies("D")
                    .status("A") // 활성
                    .emailVerified("N")
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
                .filter(user -> "A".equals(user.getStatus()))
                .count();
    }
}