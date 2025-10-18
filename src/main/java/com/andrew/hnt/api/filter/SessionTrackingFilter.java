package com.andrew.hnt.api.filter;

import com.andrew.hnt.api.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 세션 추적을 위한 Servlet Filter
 * 로그인 → 메인 페이지 흐름에서 세션 변화를 추적합니다.
 */
@Component
@Order(1) // 가장 먼저 실행되도록 설정
public class SessionTrackingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(SessionTrackingFilter.class);
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("=== SessionTrackingFilter 초기화 ===");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestURI = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        
        // 정적 리소스 요청은 로깅하지 않음
        if (isStaticResource(requestURI)) {
            logger.debug("정적 리소스 요청 스킵: {}", requestURI);
            chain.doFilter(request, response);
            return;
        }
        
        String queryString = httpRequest.getQueryString();
        String fullURL = requestURI + (queryString != null ? "?" + queryString : "");
        
        // 요청 태그 결정
        String requestTag = getRequestTag(requestURI, method);
        
        // 요청 전 세션 스냅샷
        logger.info("=== [{}] 요청 시작 ===", requestTag);
        logger.info("URL: {} {}", method, fullURL);
        logger.info("RemoteAddr: {}", httpRequest.getRemoteAddr());
        logger.info("User-Agent: {}", httpRequest.getHeader("User-Agent"));
        
        // 세션 정보 로깅
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            logSessionSnapshot(session, "요청 전", requestTag);
        } else {
            logger.info("[{}] 요청 전 세션: 없음", requestTag);
        }
        
        // 쿠키 정보 로깅
        logCookies(httpRequest, "요청 전", requestTag);
        
        // 필터 체인 실행
        chain.doFilter(request, response);
        
        // 응답 후 세션 스냅샷
        session = httpRequest.getSession(false);
        if (session != null) {
            logSessionSnapshot(session, "요청 후", requestTag);
        } else {
            logger.info("[{}] 요청 후 세션: 없음", requestTag);
        }
        
        // 응답 헤더 로깅
        logResponseHeaders(httpResponse, requestTag);
        
        logger.info("=== [{}] 요청 완료 ===", requestTag);
    }
    
    @Override
    public void destroy() {
        logger.info("=== SessionTrackingFilter 종료 ===");
    }
    
    /**
     * 정적 리소스 요청인지 확인합니다.
     * MQTT 상태 확인 같은 빈번한 API 요청도 필터링합니다.
     */
    private boolean isStaticResource(String requestURI) {
        return requestURI.startsWith("/css/") ||
               requestURI.startsWith("/js/") ||
               requestURI.startsWith("/images/") ||
               requestURI.startsWith("/fonts/") ||
               requestURI.startsWith("/static/") ||
               requestURI.endsWith(".css") ||
               requestURI.endsWith(".js") ||
               requestURI.endsWith(".png") ||
               requestURI.endsWith(".jpg") ||
               requestURI.endsWith(".jpeg") ||
               requestURI.endsWith(".gif") ||
               requestURI.endsWith(".ico") ||
               requestURI.endsWith(".woff") ||
               requestURI.endsWith(".woff2") ||
               requestURI.endsWith(".ttf") ||
               requestURI.endsWith(".eot") ||
               requestURI.endsWith(".svg") ||
               requestURI.contains("/api/mqtt/status") || // MQTT 상태 확인 API 필터링
               requestURI.contains("/admin/setSensor"); // 센서 설정 API 필터링
    }
    
    /**
     * 요청 URI와 메서드에 따라 태그를 결정합니다.
     */
    private String getRequestTag(String requestURI, String method) {
        if (requestURI.contains("/login/loginProcess")) {
            return "LOGIN_POST";
        } else if (requestURI.contains("/login/login")) {
            return "LOGIN_GET";
        } else if (requestURI.contains("/main/main")) {
            return "MAIN_GET";
        } else if (requestURI.contains("/main")) {
            return "MAIN_REDIRECT";
        } else if (requestURI.contains("/api/auth/me")) {
            return "AUTH_ME";
        } else if (requestURI.contains("/login")) {
            return "LOGIN_REDIRECT";
        } else {
            return "OTHER_" + method;
        }
    }
    
    /**
     * 세션 스냅샷을 로깅합니다.
     */
    private void logSessionSnapshot(HttpSession session, String timing, String requestTag) {
        logger.info("[{}] {} 세션 스냅샷:", requestTag, timing);
        logger.info("  - 세션 ID: {}", session.getId());
        logger.info("  - isNew: {}", session.isNew());
        logger.info("  - 생성 시간: {}", session.getCreationTime());
        logger.info("  - 마지막 접근: {}", session.getLastAccessedTime());
        logger.info("  - 최대 비활성 간격: {}초", session.getMaxInactiveInterval());
        
        // 세션 속성 로깅 (최대 10개)
        Map<String, Object> attributes = new HashMap<>();
        Enumeration<String> attributeNames = session.getAttributeNames();
        int count = 0;
        while (attributeNames.hasMoreElements() && count < 10) {
            String name = attributeNames.nextElement();
            Object value = session.getAttribute(name);
            attributes.put(name, value);
            count++;
        }
        
        logger.info("  - 세션 속성 ({}개):", attributes.size());
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 핵심 키 강조 로깅
            if (isKeySessionAttribute(key)) {
                logger.info("    🔑 {}: {}", key, value);
            } else {
                logger.info("    📝 {}: {}", key, value);
            }
        }
    }
    
    /**
     * 핵심 세션 속성 키인지 확인합니다.
     */
    private boolean isKeySessionAttribute(String key) {
        return key.equals(Constants.SESSION_USER_ID) || 
               key.equals(Constants.SESSION_USER_GRADE) || 
               key.equals(Constants.SESSION_USER_NM) ||
               key.equals("userId") || 
               key.equals("userGrade") || 
               key.equals("userNm") ||
               key.equals("loginUserId") ||
               key.equals("sensorId");
    }
    
    /**
     * 요청 쿠키를 로깅합니다.
     */
    private void logCookies(HttpServletRequest request, String timing, String requestTag) {
        javax.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            logger.info("[{}] {} 쿠키 ({}개):", requestTag, timing, cookies.length);
            for (javax.servlet.http.Cookie cookie : cookies) {
                if (cookie.getName().equals("JSESSIONID")) {
                    logger.info("  🍪 {}: {} (Domain: {}, Path: {}, Secure: {}, HttpOnly: {})", 
                        cookie.getName(), cookie.getValue(), 
                        cookie.getDomain(), cookie.getPath(), 
                        cookie.getSecure(), cookie.isHttpOnly());
                } else {
                    logger.info("  🍪 {}: {}", cookie.getName(), cookie.getValue());
                }
            }
        } else {
            logger.info("[{}] {} 쿠키: 없음", requestTag, timing);
        }
    }
    
    /**
     * 응답 헤더를 로깅합니다.
     */
    private void logResponseHeaders(HttpServletResponse response, String requestTag) {
        int status = response.getStatus();
        logger.info("[{}] 응답 상태: {}", requestTag, status);
        
        // 리다이렉트 체인 추적
        if (status >= 300 && status < 400) {
            String location = response.getHeader("Location");
            logger.info("[{}] 🔄 리다이렉트: {} → {}", requestTag, status, location);
            
            if (location != null) {
                // 동일/교차 도메인 여부 확인
                boolean isSameDomain = location.startsWith("/") || 
                                     location.contains("iot.hntsolution.co.kr");
                logger.info("[{}] 도메인: {}", requestTag, isSameDomain ? "동일" : "교차");
            }
        }
        
        // Set-Cookie 헤더 확인
        boolean hasSetCookie = response.containsHeader("Set-Cookie");
        logger.info("[{}] Set-Cookie: {}", requestTag, hasSetCookie ? "있음" : "없음");
        
        if (hasSetCookie) {
            // Set-Cookie 헤더의 모든 값 로깅
            for (String headerName : response.getHeaderNames()) {
                if ("Set-Cookie".equals(headerName)) {
                    for (String cookieValue : response.getHeaders(headerName)) {
                        logger.info("[{}] Set-Cookie: {}", requestTag, cookieValue);
                    }
                }
            }
        }
    }
}
