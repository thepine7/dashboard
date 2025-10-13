package com.andrew.hnt.api.scheduler;

import com.andrew.hnt.api.service.SessionSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 세션 정리 스케줄러
 * 만료된 세션 정보를 정기적으로 정리
 */
@Component
public class SessionCleanupScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionCleanupScheduler.class);
    
    // 세션 최대 수명 (30분 = 1800000ms)
    private static final long SESSION_MAX_AGE = 30 * 60 * 1000;
    
    @Autowired
    private SessionSecurityService sessionSecurityService;
    
    /**
     * 매 5분마다 만료된 세션 정리
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300000ms
    public void cleanupExpiredSessions() {
        try {
            int beforeCount = sessionSecurityService.getActiveSessionCount();
            sessionSecurityService.cleanupExpiredSessions(SESSION_MAX_AGE);
            int afterCount = sessionSecurityService.getActiveSessionCount();
            
            if (beforeCount != afterCount) {
                logger.info("만료된 세션 정리 완료 - 정리 전: {}, 정리 후: {}", beforeCount, afterCount);
            }
        } catch (Exception e) {
            logger.error("세션 정리 중 오류 발생", e);
        }
    }
    
    /**
     * 매 시간마다 세션 상태 로깅
     */
    @Scheduled(fixedRate = 3600000) // 1시간 = 3600000ms
    public void logSessionStatus() {
        try {
            int activeSessionCount = sessionSecurityService.getActiveSessionCount();
            logger.info("현재 활성 세션 수: {}", activeSessionCount);
        } catch (Exception e) {
            logger.error("세션 상태 로깅 중 오류 발생", e);
        }
    }
}

