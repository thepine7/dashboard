package com.andrew.hnt.api.security;

import com.andrew.hnt.api.common.Constants;
import com.andrew.hnt.api.service.SessionSecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 세션 보안 인터셉터
 * 모든 요청에 대해 세션 보안 검증 수행
 */
@Component
public class SessionSecurityInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionSecurityInterceptor.class);
    
    @Autowired
    private SessionSecurityService sessionSecurityService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.debug("=== SessionSecurityInterceptor 활성화 ===");
        
        // 정적 리소스는 제외
        String requestURI = request.getRequestURI();
        if (requestURI.startsWith("/css/") || 
            requestURI.startsWith("/js/") || 
            requestURI.startsWith("/images/") || 
            requestURI.startsWith("/fonts/") ||
            requestURI.startsWith("/health/")) {
            return true;
        }
        
        // 로그인 관련 요청은 제외
        if (requestURI.startsWith("/login/") || 
            requestURI.equals("/") || 
            requestURI.equals("")) {
            return true;
        }
        
        // 앱 요청 감지 (User-Agent 확인)
        String userAgent = request.getHeader("User-Agent");
        boolean isAppRequest = userAgent != null && (userAgent.contains("hnt_android") || userAgent.contains("okhttp"));
        
        if (isAppRequest) {
            logger.debug("앱 요청 감지 - 세션 검증 완화: {}", requestURI);
            // 앱 요청은 세션 검증을 완화 (기본 세션 체크만 수행)
            HttpSession session = request.getSession(false);
            if (session == null) {
                logger.warn("앱 요청이지만 세션이 없음 - 로그인 페이지로 리다이렉트: {}", requestURI);
                response.sendRedirect("/login/login");
                return false;
            }
            
            String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
            if (userId == null || userId.isEmpty()) {
                userId = (String) session.getAttribute("userId");
            }
            
            if (userId == null || userId.isEmpty()) {
                logger.warn("앱 요청이지만 사용자 ID가 없음 - 로그인 페이지로 리다이렉트: {}", requestURI);
                response.sendRedirect("/login/login");
                return false;
            }
            
            // 앱 요청은 보안 검증을 건너뛰고 통과
            logger.debug("앱 요청 세션 검증 완료 - userId: {}", userId);
            return true;
        }
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            // 세션이 없는 경우 로그인 페이지로 리다이렉트
            logger.warn("세션이 없음 - 로그인 페이지로 리다이렉트: {}", requestURI);
            response.sendRedirect("/login/login");
            return false;
        }
        
        // Constants 키 우선, 없으면 기본 키 사용
        String userId = (String) session.getAttribute(Constants.SESSION_USER_ID);
        if (userId == null || userId.isEmpty()) {
            userId = (String) session.getAttribute("userId");
        }
        
        if (userId == null || userId.isEmpty()) {
            // 사용자 ID가 없는 경우 로그인 페이지로 리다이렉트
            logger.warn("사용자 ID가 없음 - 로그인 페이지로 리다이렉트: {}", requestURI);
            response.sendRedirect("/login/login");
            return false;
        }
        
        // 세션 보안 검증
        if (!sessionSecurityService.validateSessionSecurity(session, request)) {
            logger.warn("Session security validation failed for user: {}", userId);
            
            // 세션 무효화 후 로그인 페이지로 리다이렉트
            session.invalidate();
            response.sendRedirect("/login/login?error=session_invalid");
            return false;
        }
        
        // 세션 갱신
        sessionSecurityService.refreshSession(session);
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 요청 완료 후 정리 작업
        if (ex != null) {
            logger.error("Request processing error: {}", ex.getMessage(), ex);
        }
    }
}

