package com.andrew.hnt.api.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 세션 보안 서비스 인터페이스
 * 통합된 세션 보안 관리
 */
public interface SessionSecurityService {
    
    /**
     * 세션 보안 검증
     * @param session HttpSession
     * @param request HttpServletRequest
     * @return boolean
     */
    boolean validateSessionSecurity(HttpSession session, HttpServletRequest request);
    
    /**
     * 세션 무효화
     * @param session HttpSession
     */
    void invalidateSession(HttpSession session);
    
    /**
     * 세션 정보 설정
     * @param session HttpSession
     * @param request HttpServletRequest
     * @param userId 사용자 ID
     * @param userGrade 사용자 등급
     * @param userNm 사용자명
     */
    void setSessionInfo(HttpSession session, HttpServletRequest request, String userId, String userGrade, String userNm);
    
    /**
     * 세션 갱신
     * @param session HttpSession
     */
    void refreshSession(HttpSession session);
    
    /**
     * 세션 통계 정보 반환
     * @return SessionStats
     */
    SessionStats getSessionStats();
    
    /**
     * 활성 세션 수 반환
     * @return int
     */
    int getActiveSessionCount();
    
    /**
     * 만료된 세션 정리
     * @param maxAgeMs 최대 세션 수명 (밀리초)
     * @return int 정리된 세션 수
     */
    int cleanupExpiredSessions(long maxAgeMs);
    
    /**
     * 세션 정보 제거
     * @param userId 사용자 ID
     */
    void removeSessionInfo(String userId);
    
    /**
     * 세션 통계 데이터 클래스
     */
    class SessionStats {
        private final int activeSessions;
        private final int expiredSessions;
        private final int securityViolations;
        private final long lastCleanupTime;
        
        public SessionStats(int activeSessions, int expiredSessions, int securityViolations, long lastCleanupTime) {
            this.activeSessions = activeSessions;
            this.expiredSessions = expiredSessions;
            this.securityViolations = securityViolations;
            this.lastCleanupTime = lastCleanupTime;
        }
        
        // Getters
        public int getActiveSessions() { return activeSessions; }
        public int getExpiredSessions() { return expiredSessions; }
        public int getSecurityViolations() { return securityViolations; }
        public long getLastCleanupTime() { return lastCleanupTime; }
    }
}
