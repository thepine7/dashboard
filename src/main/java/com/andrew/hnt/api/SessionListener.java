package com.andrew.hnt.api;

import com.andrew.hnt.api.model.LoginVO;
import com.andrew.hnt.api.service.LoginService;
import com.andrew.hnt.api.service.SessionSecurityService;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionListener implements HttpSessionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionListener.class);
    private static final java.util.Set<String> processedSessions = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());
    
    private int sessionTime = 1800; // 기본값 30분
    private LoginService loginService;
    
    @Autowired
    private SessionSecurityService sessionSecurityService;
    
    // 생성자 주입을 위한 생성자
    public SessionListener() {
        // 기본 생성자
    }
    
    public SessionListener(int sessionTime, LoginService loginService) {
        this.sessionTime = sessionTime;
        this.loginService = loginService;
    }

    public void sessionCreated(HttpSessionEvent se) {
        String sessionId = se.getSession().getId();
        
        // 동시성 안전한 중복 처리 방지
        synchronized (processedSessions) {
            if (processedSessions.contains(sessionId)) {
                logger.warn("세션 생성 중복 호출 감지 - 세션 ID: {}, 무시됨", sessionId);
                return;
            }
            processedSessions.add(sessionId);
        }
        
        try {
            se.getSession().setMaxInactiveInterval(sessionTime);
            logger.info("세션 생성됨 - 세션 ID: {}, 타임아웃: {}초", sessionId, sessionTime);
        } catch (Exception e) {
            logger.error("세션 생성 중 오류 발생 - 세션 ID: {}, 오류: {}", sessionId, e.getMessage());
            // 오류 발생 시 처리된 세션 목록에서 제거
            synchronized (processedSessions) {
                processedSessions.remove(sessionId);
            }
        }
    }

    public void sessionDestroyed(HttpSessionEvent se) {
        HttpSession session = se.getSession();
        String sessionId = session.getId();
        String userId = (String) session.getAttribute("userId");
        
        // 동시성 안전한 세션 목록에서 제거
        synchronized (processedSessions) {
            processedSessions.remove(sessionId);
        }
        
        try {
            if (userId != null && !userId.isEmpty()) {
                // 세션 보안 정보 제거
                if (sessionSecurityService != null) {
                    sessionSecurityService.removeSessionInfo(userId);
                }
                
                if (loginService != null) {
                    try {
                        LoginVO loginVO = new LoginVO();
                        loginVO.setUserId(userId);
                        loginService.updateLogoutDtm(loginVO);
                        logger.info("세션 만료로 인한 자동 로그아웃 처리 완료 - userId: {}, 세션 ID: {}", userId, sessionId);
                    } catch (Exception e) {
                        logger.error("세션 만료 시 로그아웃 처리 실패 - userId: {}, 세션 ID: {}, error: {}", userId, sessionId, e.getMessage());
                    }
                } else {
                    logger.warn("LoginService가 null입니다. 세션 만료 로그만 기록 - userId: {}, 세션 ID: {}", userId, sessionId);
                }
            } else {
                logger.info("세션 만료됨 - 사용자 정보 없음, 세션 ID: {}", sessionId);
            }
        } catch (Exception e) {
            logger.error("세션 만료 처리 중 오류 발생 - 세션 ID: {}, 오류: {}", sessionId, e.getMessage());
        }
    }
}