package com.andrew.hnt.api.service.impl;

import com.andrew.hnt.api.service.SessionSecurityService;
import com.andrew.hnt.api.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 세션 보안 서비스 구현체
 * 통합된 세션 보안 관리 및 모니터링
 */
@Service
public class SessionSecurityServiceImpl implements SessionSecurityService {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionSecurityServiceImpl.class);
    
    // 세션 통계
    private final AtomicInteger activeSessions = new AtomicInteger(0);
    private final AtomicInteger expiredSessions = new AtomicInteger(0);
    private final AtomicInteger securityViolations = new AtomicInteger(0);
    private final AtomicLong lastCleanupTime = new AtomicLong(System.currentTimeMillis());
    
    // 활성 세션 추적
    private final ConcurrentHashMap<String, SessionInfo> activeSessionMap = new ConcurrentHashMap<>();
    
    // 세션 설정
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000; // 30분
    private static final long MAX_SESSION_AGE_MS = 12 * 60 * 60 * 1000; // 12시간
    
    /**
     * 세션 보안 검증
     */
    @Override
    public boolean validateSessionSecurity(HttpSession session, HttpServletRequest request) {
        if (session == null) {
            logger.warn("세션이 null임");
            return false;
        }
        
        try {
            String userId = (String) session.getAttribute("userId");
            logger.debug("세션 보안 검증 시작 - userId: {}, sessionId: {}", userId, session.getId());
            
            // 기본 세션 유효성 검사
            if (!isValidSession(session)) {
                logger.warn("기본 세션 유효성 검사 실패 - userId: {}", userId);
                return false;
            }
            
            // IP 주소 검증
            if (!validateClientIP(session, request)) {
                logger.warn("IP 주소 검증 실패 - userId: {}", userId);
                securityViolations.incrementAndGet();
                return false;
            }
            
            // User-Agent 검증
            if (!validateUserAgent(session, request)) {
                logger.warn("User-Agent 검증 실패 - userId: {}", userId);
                securityViolations.incrementAndGet();
                return false;
            }
            
            // 세션 생성 시간 검증
            if (!validateSessionAge(session)) {
                return false;
            }
            
            // 마지막 접근 시간 검증
            if (!validateLastAccessTime(session)) {
                return false;
            }
            
            // 세션 갱신
            refreshSession(session);
            
            return true;
            
        } catch (Exception e) {
            logger.error("세션 보안 검증 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 기본 세션 유효성 검사
     */
    private boolean isValidSession(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        String userGrade = (String) session.getAttribute("userGrade");
        
        if (StringUtil.isEmpty(userId) || StringUtil.isEmpty(userGrade)) {
            logger.warn("세션에 필수 정보가 없음 - userId: {}, userGrade: {}", userId, userGrade);
            return false;
        }
        
        return true;
    }
    
    /**
     * 클라이언트 IP 주소 검증 (완화된 버전)
     */
    private boolean validateClientIP(HttpSession session, HttpServletRequest request) {
        if (request == null) {
            return true; // 요청이 없으면 검증 생략
        }
        
        String currentIP = getClientIPAddress(request);
        String sessionIP = (String) session.getAttribute("clientIP");
        
        // 세션에 IP가 없으면 현재 IP로 설정하고 통과
        if (StringUtil.isEmpty(sessionIP)) {
            session.setAttribute("clientIP", currentIP);
            logger.info("세션에 IP 주소 설정 - IP: {}", currentIP);
            return true;
        }
        
        // IP 주소가 다르면 경고 로그만 남기고 통과 (세션 무효화하지 않음)
        if (!sessionIP.equals(currentIP)) {
            logger.warn("IP 주소 변경 감지 - 세션 IP: {}, 현재 IP: {} (세션 유지)", sessionIP, currentIP);
            // IP 주소 업데이트
            session.setAttribute("clientIP", currentIP);
            return true; // false에서 true로 변경 - 세션 무효화하지 않음
        }
        
        return true;
    }
    
    /**
     * User-Agent 검증 (완화된 버전)
     */
    private boolean validateUserAgent(HttpSession session, HttpServletRequest request) {
        if (request == null) {
            return true; // 요청이 없으면 검증 생략
        }
        
        String currentUserAgent = request.getHeader("User-Agent");
        String sessionUserAgent = (String) session.getAttribute("userAgent");
        
        // 세션에 User-Agent가 없으면 현재 User-Agent로 설정하고 통과
        if (StringUtil.isEmpty(sessionUserAgent)) {
            session.setAttribute("userAgent", currentUserAgent);
            logger.info("세션에 User-Agent 설정 - User-Agent: {}", currentUserAgent);
            return true;
        }
        
        // User-Agent가 다르면 경고 로그만 남기고 통과 (세션 무효화하지 않음)
        if (!sessionUserAgent.equals(currentUserAgent)) {
            logger.warn("User-Agent 변경 감지 - 세션 UA: {}, 현재 UA: {} (세션 유지)", sessionUserAgent, currentUserAgent);
            // User-Agent 업데이트
            session.setAttribute("userAgent", currentUserAgent);
            return true; // false에서 true로 변경 - 세션 무효화하지 않음
        }
        
        return true;
    }
    
    /**
     * 세션 생성 시간 검증
     */
    private boolean validateSessionAge(HttpSession session) {
        Long sessionCreationTime = (Long) session.getAttribute("creationTime");
        if (sessionCreationTime != null) {
            long currentTime = System.currentTimeMillis();
            long sessionAge = currentTime - sessionCreationTime;
            
            if (sessionAge > MAX_SESSION_AGE_MS) {
                logger.warn("세션 만료 - 생성 시간: {}, 현재 시간: {}, 경과 시간: {}ms", 
                           sessionCreationTime, currentTime, sessionAge);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 마지막 접근 시간 검증
     */
    private boolean validateLastAccessTime(HttpSession session) {
        long lastAccessedTime = session.getLastAccessedTime();
        long currentTime = System.currentTimeMillis();
        long timeSinceLastAccess = currentTime - lastAccessedTime;
        
        if (timeSinceLastAccess > SESSION_TIMEOUT_MS) {
            logger.warn("세션 타임아웃 - 마지막 접근: {}, 현재: {}, 경과 시간: {}ms", 
                       lastAccessedTime, currentTime, timeSinceLastAccess);
            return false;
        }
        
        return true;
    }
    
    /**
     * 클라이언트 IP 주소 가져오기
     */
    private String getClientIPAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtil.isNotEmpty(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (StringUtil.isNotEmpty(xRealIP)) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 세션 무효화
     */
    @Override
    public void invalidateSession(HttpSession session) {
        if (session != null) {
            try {
                String userId = (String) session.getAttribute("userId");
                String sessionId = session.getId();
                
                logger.info("세션 무효화 - userId: {}, sessionId: {}", userId, sessionId);
                
                // 활성 세션에서 제거
                activeSessionMap.remove(sessionId);
                activeSessions.decrementAndGet();
                expiredSessions.incrementAndGet();
                
                // 세션 완전 무효화
                session.invalidate();
                
            } catch (Exception e) {
                logger.error("세션 무효화 중 오류 발생", e);
            }
        }
    }
    
    /**
     * 세션 정보 설정
     */
    @Override
    public void setSessionInfo(HttpSession session, HttpServletRequest request, String userId, String userGrade, String userNm) {
        if (session == null) {
            logger.warn("세션이 null이므로 정보 설정 불가");
            return;
        }
        
        try {
            // 보안 정보만 설정 (기본 사용자 정보는 SessionManagementService에서 처리)
            // session.setAttribute("userId", userId); // SessionManagementService에서 설정
            // session.setAttribute("userGrade", userGrade); // SessionManagementService에서 설정
            // session.setAttribute("userNm", userNm); // SessionManagementService에서 설정
            
            // 보안 정보 설정
            if (request != null) {
                String clientIP = getClientIPAddress(request);
                String userAgent = request.getHeader("User-Agent");
                
                session.setAttribute("clientIP", clientIP);
                session.setAttribute("userAgent", userAgent);
            }
            
            // 세션 생성 시간 설정
            session.setAttribute("creationTime", System.currentTimeMillis());
            
            // 활성 세션 추적
            String sessionId = session.getId();
            SessionInfo sessionInfo = new SessionInfo(sessionId, userId, userGrade, System.currentTimeMillis());
            activeSessionMap.put(sessionId, sessionInfo);
            activeSessions.incrementAndGet();
            
            logger.info("세션 정보 설정 완료 - userId: {}, userGrade: {}, userNm: {}, sessionId: {}", 
                       userId, userGrade, userNm, sessionId);
            
        } catch (Exception e) {
            logger.error("세션 정보 설정 중 오류 발생", e);
        }
    }
    
    /**
     * 세션 갱신
     */
    @Override
    public void refreshSession(HttpSession session) {
        if (session != null) {
            try {
                String userId = (String) session.getAttribute("userId");
                String sessionId = session.getId();
                
                // 활성 세션 정보 업데이트
                SessionInfo sessionInfo = activeSessionMap.get(sessionId);
                if (sessionInfo != null) {
                    sessionInfo.setLastAccessTime(System.currentTimeMillis());
                }
                
                logger.debug("세션 갱신 - userId: {}, sessionId: {}", userId, sessionId);
                
            } catch (Exception e) {
                logger.error("세션 갱신 중 오류 발생", e);
            }
        }
    }
    
    /**
     * 세션 통계 정보 반환
     */
    @Override
    public SessionStats getSessionStats() {
        return new SessionStats(
            activeSessions.get(),
            expiredSessions.get(),
            securityViolations.get(),
            lastCleanupTime.get()
        );
    }
    
    /**
     * 5분마다 세션 정리 작업
     */
    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredSessions() {
        try {
            long currentTime = System.currentTimeMillis();
            int cleanedCount = 0;
            
            // 만료된 세션 정리
            activeSessionMap.entrySet().removeIf(entry -> {
                SessionInfo sessionInfo = entry.getValue();
                long timeSinceLastAccess = currentTime - sessionInfo.getLastAccessTime();
                
                if (timeSinceLastAccess > SESSION_TIMEOUT_MS) {
                    logger.debug("만료된 세션 정리 - sessionId: {}, userId: {}", 
                               entry.getKey(), sessionInfo.getUserId());
                    activeSessions.decrementAndGet();
                    expiredSessions.incrementAndGet();
                    return true;
                }
                return false;
            });
            
            lastCleanupTime.set(currentTime);
            
            if (cleanedCount > 0) {
                logger.info("세션 정리 완료 - 정리된 세션 수: {}", cleanedCount);
            }
            
        } catch (Exception e) {
            logger.error("세션 정리 중 오류 발생", e);
        }
    }
    
    /**
     * 활성 세션 수 반환
     */
    @Override
    public int getActiveSessionCount() {
        return activeSessions.get();
    }
    
    /**
     * 만료된 세션 정리
     */
    @Override
    public int cleanupExpiredSessions(long maxAgeMs) {
        try {
            long currentTime = System.currentTimeMillis();
            int cleanedCount = 0;
            
            // 만료된 세션 정리
            activeSessionMap.entrySet().removeIf(entry -> {
                SessionInfo sessionInfo = entry.getValue();
                long timeSinceLastAccess = currentTime - sessionInfo.getLastAccessTime();
                
                if (timeSinceLastAccess > maxAgeMs) {
                    logger.debug("만료된 세션 정리 - sessionId: {}, userId: {}", 
                               entry.getKey(), sessionInfo.getUserId());
                    activeSessions.decrementAndGet();
                    expiredSessions.incrementAndGet();
                    return true;
                }
                return false;
            });
            
            lastCleanupTime.set(currentTime);
            
            if (cleanedCount > 0) {
                logger.info("세션 정리 완료 - 정리된 세션 수: {}", cleanedCount);
            }
            
            return cleanedCount;
            
        } catch (Exception e) {
            logger.error("세션 정리 중 오류 발생", e);
            return 0;
        }
    }
    
    /**
     * 세션 정보 제거
     */
    @Override
    public void removeSessionInfo(String userId) {
        try {
            if (StringUtil.isNotEmpty(userId)) {
                // 사용자 ID로 세션 찾아서 제거
                activeSessionMap.entrySet().removeIf(entry -> {
                    SessionInfo sessionInfo = entry.getValue();
                    if (userId.equals(sessionInfo.getUserId())) {
                        activeSessions.decrementAndGet();
                        logger.debug("세션 정보 제거 - userId: {}", userId);
                        return true;
                    }
                    return false;
                });
            }
        } catch (Exception e) {
            logger.error("세션 정보 제거 중 오류 발생 - userId: {}", userId, e);
        }
    }
    
    /**
     * 세션 정보 클래스
     */
    private static class SessionInfo {
        private final String userId;
        private long lastAccessTime;
        
        public SessionInfo(String sessionId, String userId, String userGrade, long creationTime) {
            this.userId = userId;
            this.lastAccessTime = creationTime;
        }
        
        // Getters and Setters
        public String getUserId() { return userId; }
        public long getLastAccessTime() { return lastAccessTime; }
        public void setLastAccessTime(long lastAccessTime) { this.lastAccessTime = lastAccessTime; }
    }
}
