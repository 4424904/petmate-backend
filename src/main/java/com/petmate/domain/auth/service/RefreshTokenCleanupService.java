package com.petmate.domain.auth.service;

import com.petmate.domain.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionManagementService sessionManagementService;

    /**
     * 매일 새벽 3시에 만료된 RefreshToken과 비활성 세션 정리
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting cleanup of expired refresh tokens and inactive sessions...");

        // 만료된 토큰 정리
        int expiredCount = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());

        // 30분 비활성 세션 정리
        int inactiveCount = sessionManagementService.cleanupInactiveSessions();

        log.info("Cleanup completed. Deleted {} expired tokens and {} inactive sessions",
                expiredCount, inactiveCount);
    }

    /**
     * 10분마다 비활성 세션 정리 (실시간)
     */
    @Scheduled(fixedRate = 600000) // 10분 = 600,000ms
    @Transactional
    public void cleanupInactiveSessionsRealtime() {
        int inactiveCount = sessionManagementService.cleanupInactiveSessions();

        if (inactiveCount > 0) {
            log.info("Realtime cleanup: removed {} inactive sessions", inactiveCount);
        }
    }

    /**
     * 수동 정리 메서드 (관리자 API 등에서 호출 가능)
     */
    @Transactional
    public int manualCleanup() {
        log.info("Manual cleanup of expired refresh tokens requested");

        int deletedCount = refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());

        log.info("Manual cleanup completed. Deleted {} expired refresh tokens", deletedCount);
        return deletedCount;
    }
}