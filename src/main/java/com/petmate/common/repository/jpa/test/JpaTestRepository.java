package com.petmate.common.repository.jpa.test;

import com.petmate.common.entity.test.JpaTest;
import com.petmate.common.entity.test.JpaTestProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaTestRepository extends JpaRepository<JpaTest, Long> {

    // 이메일로 테스트 데이터 찾기
    Optional<JpaTest> findByEmail(String email);

    // 소셜 정보로 테스트 데이터 찾기
    Optional<JpaTest> findByProviderAndProviderId(JpaTestProvider provider, String providerId);

    // 활성 데이터만 조회
    @Query("SELECT j FROM JpaTest j WHERE j.email = :email AND j.isActive = true")
    Optional<JpaTest> findActiveByEmail(@Param("email") String email);

    // 이메일 존재 여부 확인
    boolean existsByEmail(String email);

    // 소셜 계정 존재 여부 확인
    boolean existsByProviderAndProviderId(JpaTestProvider provider, String providerId);
}