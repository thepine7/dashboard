package com.andrew.hnt.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 보안 헤더 필터
 * XSS, CSRF 방지 및 보안 정책 강화를 위한 헤더 추가
 */
@Component
public class SecurityHeaderFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityHeaderFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("보안 헤더 필터 초기화 완료");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        try {
            // 보안 헤더 추가
            addSecurityHeaders(httpResponse, httpRequest);
            
            // 요청 처리 계속
            chain.doFilter(request, response);
            
        } catch (Exception e) {
            logger.error("보안 헤더 필터 처리 중 오류 발생", e);
            throw e;
        }
    }

    @Override
    public void destroy() {
        logger.info("보안 헤더 필터 종료");
    }

    /**
     * 보안 헤더 추가
     */
    private void addSecurityHeaders(HttpServletResponse response, HttpServletRequest request) {
        // XSS 방지 헤더
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
        
        // Content Security Policy (CSP)
        response.setHeader("Content-Security-Policy", 
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdnjs.cloudflare.com https://code.jquery.com; " +
            "style-src 'self' 'unsafe-inline' https://cdnjs.cloudflare.com; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' https://cdnjs.cloudflare.com; " +
            "connect-src 'self' wss: ws:; " +
            "frame-ancestors 'none'; " +
            "base-uri 'self'; " +
            "form-action 'self'");
        
        // Referrer Policy
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        
        // Permissions Policy (Feature Policy) - 브라우저 호환성 개선
        response.setHeader("Permissions-Policy", 
            "geolocation=(), " +
            "microphone=(), " +
            "camera=(), " +
            "payment=(), " +
            "usb=(), " +
            "magnetometer=(), " +
            "gyroscope=(), " +
            "fullscreen=(self), " +
            "sync-xhr=()");
        
        // Strict Transport Security (HTTPS 사용 시)
        if (isHttpsRequest(request)) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");
        }
        
        // Cache Control
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        // Server 정보 숨기기
        response.setHeader("Server", "HnT-Sensor-API");
        
        // Cross-Origin 정책 - HTTPS 환경에서만 적용
        if (isHttpsRequest(request)) {
            response.setHeader("Cross-Origin-Embedder-Policy", "require-corp");
            response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
            response.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        } else {
            // HTTP 환경에서는 더 관대한 정책 적용
            response.setHeader("Cross-Origin-Opener-Policy", "unsafe-none");
        }
    }

    /**
     * HTTPS 요청 여부 확인
     */
    private boolean isHttpsRequest(HttpServletRequest request) {
        // 직접 HTTPS 체크
        if (request.isSecure()) {
            return true;
        }
        
        // 프록시를 통한 HTTPS 체크
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedProto != null && forwardedProto.equals("https")) {
            return true;
        }
        
        // CloudFlare 등의 CDN을 통한 HTTPS 체크
        String cfVisitor = request.getHeader("CF-Visitor");
        if (cfVisitor != null && cfVisitor.contains("https")) {
            return true;
        }
        
        return false;
    }
}
