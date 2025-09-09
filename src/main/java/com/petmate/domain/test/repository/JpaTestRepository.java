package com.petmate.domain.test.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.petmate.domain.user.entity.UserEntity;

import java.util.Optional;

@Repository
public interface JpaTestRepository extends JpaRepository<UserEntity, Integer> {

    // 이메일로 테스트 데이터 찾기
    Optional<UserEntity> findByEmail(String email);

    // 소셜 정보로 테스트 데이터 찾기
    Optional<UserEntity> findByProviderAndName(String provider, String name);

    // 활성 데이터만 조회 (status가 'A'인 경우)
    @Query("SELECT u FROM UserEntity u WHERE u.email = :email AND u.status = 'A'")
    Optional<UserEntity> findActiveByEmail(@Param("email") String email);

    // 이메일 존재 여부 확인
    boolean existsByEmail(String email);

    // 소셜 계정 존재 여부 확인
    boolean existsByProviderAndName(String provider, String name);
}