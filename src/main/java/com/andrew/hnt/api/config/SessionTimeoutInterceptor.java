package com.andrew.hnt.api.config;

import com.andrew.hnt.api.service.SessionManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 세션 타임아웃 처리 인터셉터
 * 세션 만료 시 적절한 응답을 제공
 */
@Component
public class SessionTimeoutInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionTimeoutInterceptor.class);
    
    @Autowired
    private SessionManagementService sessionManagementService;
    
    // 세션 타임아웃 체크를 제외할 경로들
    private static final String[] EXCLUDE_PATHS = {
        "/login/login",          // 로그인 페이지
        "/login/join",           // 회원가입 페이지
        "/login/loginProcess",   // 로그인 처리
        "/login/joinProcess",    // 회원가입 처리
        "/static/",
        "/css/",
        "/js/",
        "/images/",
        "/fonts/",
        "/api/realtime/connect",
        "/api/mqtt/status",
        "/api/health",
        "/api/heartbeat",        // 앱 하트비트 (세션 없이 userId로 검증)
        "/data/getSensorList",   // 앱 장치 목록 조회 (세션 없이 userId로 검증)
        "/admin/setSensor",      // 앱 MQTT 메시지 발행 (세션 없이 userId로 검증)
        "/test/"                 // 테스트 엔드포인트 (세션 없이 접근 가능)
    };
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.debug("=== SessionTimeoutInterceptor 활성화 ===");
        
        String requestURI = request.getRequestURI();
        
        // 제외 경로 확인
        for (String excludePath : EXCLUDE_PATHS) {
            if (requestURI.startsWith(excludePath)) {
                return true;
            }
        }
        
        // 앱 요청 감지 (User-Agent 확인)
        String userAgent = request.getHeader("User-Agent");
        boolean isAppRequest = userAgent != null && (userAgent.contains("hnt_android") || userAgent.contains("okhttp"));
        
        if (isAppRequest) {
            logger.debug("앱 요청 감지 - 세션 검증 건너뜀: {}", requestURI);
            return true; // 앱 요청은 세션 검증 건너뜀
        }
        
        HttpSession session = request.getSession(false);
        if (session == null) {
            // 세션이 없는 경우 로그인 페이지로 리다이렉트
            logger.warn("세션이 없음 - 타임아웃으로 인한 리다이렉트: {}", requestURI);
            response.sendRedirect("/login/login?timeout=true");
            return false;
        }
        
        // 세션 타임아웃 체크 (30분)
        long lastAccessedTime = session.getLastAccessedTime();
        long currentTime = System.currentTimeMillis();
        long sessionTimeout = 30 * 60 * 1000; // 30분
        
        if (currentTime - lastAccessedTime > sessionTimeout) {
            logger.warn("세션 타임아웃 - 마지막 접근: {}, 현재: {}, URI: {}", 
                lastAccessedTime, currentTime, requestURI);
            
            // 세션 무효화
            session.invalidate();
            response.sendRedirect("/login/login?timeout=true");
            return false;
        }
        
        // 세션 갱신
        session.setAttribute("lastAccessTime", currentTime);
        
        return true;
    }
    
    /**
     * 제외 경로인지 확인
     * @param requestURI 요청 URI
     * @return boolean
     */
    private boolean isExcludedPath(String requestURI) {
        for (String excludePath : EXCLUDE_PATHS) {
            if (requestURI.startsWith(excludePath)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * AJAX 요청인지 확인
     * @param request HttpServletRequest
     * @return boolean
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String contentType = request.getHeader("Content-Type");
        
        return "XMLHttpRequest".equals(requestedWith) || 
               (contentType != null && contentType.contains("application/json"));
    }
    
    /**
     * AJAX 에러 응답 전송
     * @param response HttpServletResponse
     * @param message 에러 메시지
     * @throws IOException
     */
    private void sendAjaxErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        
        String jsonResponse = String.format(
            "{\"resultCode\":\"401\",\"resultMessage\":\"%s\",\"redirect\":\"/login\"}", 
            message
        );
        
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
    
    /**
     * 클라이언트 IP 주소 가져오기
     * @param request HttpServletRequest
     * @return String
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

