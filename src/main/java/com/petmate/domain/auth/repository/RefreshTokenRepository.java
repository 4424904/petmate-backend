package com.petmate.domain.auth.repository;

import com.petmate.domain.auth.entity.RefreshTokenEntity;
import com.petmate.domain.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByToken(String token);

    List<RefreshTokenEntity> findByUser(UserEntity user);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.token = :token")
    void deleteByToken(@Param("token") String token);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.user = :user")
    void deleteByUser(@Param("user") UserEntity user);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.lastAccessedAt < :inactiveTime")
    int deleteInactiveTokens(@Param("inactiveTime") LocalDateTime inactiveTime);

    @Query("SELECT COUNT(rt) FROM RefreshTokenEntity rt WHERE rt.user = :user")
    long countByUser(@Param("user") UserEntity user);

    @Query("SELECT rt FROM RefreshTokenEntity rt WHERE rt.user = :user ORDER BY rt.createdAt DESC")
    List<RefreshTokenEntity> findByUserOrderByCreatedAtDesc(@Param("user") UserEntity user);

    List<RefreshTokenEntity> findByUser_Id(Long userId);
}