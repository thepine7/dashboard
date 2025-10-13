package com.andrew.hnt.api.interceptor;

import com.andrew.hnt.api.service.PerformanceMonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 성능 모니터링 인터셉터
 * HTTP 요청의 성능 지표를 수집하고 모니터링
 */
public class PerformanceInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceInterceptor.class);

    private final PerformanceMonitoringService performanceMonitoringService;

    public PerformanceInterceptor() {
        this.performanceMonitoringService = null; // WebConfig에서 주입받도록 수정
    }

    public PerformanceInterceptor(PerformanceMonitoringService performanceMonitoringService) {
        this.performanceMonitoringService = performanceMonitoringService;
    }

    private static final ThreadLocal<Long> startTime = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 요청 시작 시간 기록
        startTime.set(System.currentTimeMillis());
        
        // 요청 카운터 증가 (performanceMonitoringService가 null이 아닐 때만)
        if (performanceMonitoringService != null) {
            String endpoint = getEndpoint(request);
            performanceMonitoringService.incrementRequestCounter(endpoint);
        }
        
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            // 응답 시간 계산 및 기록
            Long start = startTime.get();
            if (start != null) {
                long responseTime = System.currentTimeMillis() - start;
                String endpoint = getEndpoint(request);
                
                // performanceMonitoringService가 null이 아닐 때만 기록
                if (performanceMonitoringService != null) {
                    performanceMonitoringService.recordResponseTime(endpoint, responseTime);
                }
                
                // 느린 요청 로깅
                if (responseTime > 5000) { // 5초 이상
                    logger.warn("느린 요청 감지: {} {} - {}ms", 
                        request.getMethod(), endpoint, responseTime);
                }
            }
        } finally {
            // ThreadLocal 정리
            startTime.remove();
        }
    }

    /**
     * 엔드포인트 식별자 생성
     */
    private String getEndpoint(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        
        // REST API 패턴 매칭
        if (uri.startsWith("/admin/")) {
            return method + " " + uri;
        } else if (uri.startsWith("/main")) {
            return "GET /main";
        } else if (uri.startsWith("/login")) {
            return "GET /login";
        } else if (uri.startsWith("/chart")) {
            return "GET /chart";
        } else {
            return method + " " + uri;
        }
    }
}
