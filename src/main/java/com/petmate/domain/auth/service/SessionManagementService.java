package com.petmate.domain.auth.service;

import com.petmate.domain.auth.entity.RefreshTokenEntity;
import com.petmate.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionManagementService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.session.inactive-timeout-minutes:30}")
    private int inactiveTimeoutMinutes;

    /**
     * 사용자 세션 활성화 (API 호출 시마다 호출)
     */
    @Transactional
    public void updateSessionActivity(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        Optional<RefreshTokenEntity> tokenOpt = refreshTokenRepository.findByToken(refreshToken);
        if (tokenOpt.isPresent()) {
            RefreshTokenEntity token = tokenOpt.get();

            // 30분 비활성 체크
            if (token.isInactive(inactiveTimeoutMinutes)) {
                log.info("Inactive session detected for token, deleting: {}", refreshToken.substring(0, 10) + "...");
                refreshTokenRepository.delete(token);
                return;
            }

            // 마지막 접근 시간 업데이트
            token.updateLastAccessed();
            refreshTokenRepository.save(token);
            log.debug("Session activity updated for token: {}", refreshToken.substring(0, 10) + "...");
        }
    }

    /**
     * JWT에서 사용자 ID로 세션 활성화 (Access Token 기반)
     */
    @Transactional
    public void updateSessionActivityByUserId(Long userId) {
        var tokens = refreshTokenRepository.findByUser_Id(userId);

        for (RefreshTokenEntity token : tokens) {
            if (token.isInactive(inactiveTimeoutMinutes)) {
                log.info("Inactive session detected for userId {}, deleting token", userId);
                refreshTokenRepository.delete(token);
            } else {
                token.updateLastAccessed();
                refreshTokenRepository.save(token);
                log.debug("Session activity updated for userId: {}", userId);
            }
        }
    }

    /**
     * 비활성 세션 정리 (스케줄러에서 호출)
     */
    @Transactional
    public int cleanupInactiveSessions() {
        LocalDateTime inactiveTime = LocalDateTime.now().minusMinutes(inactiveTimeoutMinutes);
        int deletedCount = refreshTokenRepository.deleteInactiveTokens(inactiveTime);

        if (deletedCount > 0) {
            log.info("Cleaned up {} inactive sessions (inactive for more than {} minutes)",
                    deletedCount, inactiveTimeoutMinutes);
        }

        return deletedCount;
    }

    /**
     * 세션이 유효한지 확인
     */
    @Transactional(readOnly = true)
    public boolean isSessionActive(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return false;
        }

        Optional<RefreshTokenEntity> tokenOpt = refreshTokenRepository.findByToken(refreshToken);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        RefreshTokenEntity token = tokenOpt.get();
        return !token.isExpired() && !token.isInactive(inactiveTimeoutMinutes);
    }
}