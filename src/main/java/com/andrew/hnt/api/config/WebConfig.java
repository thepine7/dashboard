package com.andrew.hnt.api.config;

import com.andrew.hnt.api.interceptor.PerformanceInterceptor;
import com.andrew.hnt.api.interceptor.SessionTrackingInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 * 웹 설정
 * 인터셉터, 필터, 정적 리소스 등 웹 관련 설정
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public PerformanceInterceptor performanceInterceptor() {
        return new PerformanceInterceptor();
    }
    
    @Bean
    public SessionTrackingInterceptor sessionTrackingInterceptor() {
        return new SessionTrackingInterceptor();
    }

    /**
     * JSP 뷰 리졸버 설정
     */
    @Bean
    public InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/jsp/");
        resolver.setSuffix(".jsp");
        resolver.setOrder(1);
        return resolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 정적 리소스 핸들러 설정 (Spring Boot WAR 배포 환경 최적화)
        // 캐시 비활성화로 항상 최신 파일 서빙
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(0); // 캐시 비활성화
        
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(0); // 캐시 비활성화
        
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCachePeriod(0); // 캐시 비활성화
        
        registry.addResourceHandler("/fonts/**")
                .addResourceLocations("classpath:/static/fonts/")
                .setCachePeriod(0); // 캐시 비활성화
        
        registry.addResourceHandler("/jqvmap/**")
                .addResourceLocations("classpath:/static/jqvmap/")
                .setCachePeriod(0); // 캐시 비활성화
        
        registry.addResourceHandler("/plugins/**")
                .addResourceLocations("classpath:/static/plugins/")
                .setCachePeriod(0); // 캐시 비활성화
        
        registry.addResourceHandler("/dist/**")
                .addResourceLocations("classpath:/static/dist/")
                .setCachePeriod(0); // 캐시 비활성화
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 세션 추적 인터셉터 등록 (로그인/메인 페이지 추적)
        registry.addInterceptor(sessionTrackingInterceptor())
                .addPathPatterns(
                    "/login/loginProcess",
                    "/main/main",
                    "/api/auth/me"
                );
        
        // 성능 모니터링 인터셉터 등록
        registry.addInterceptor(performanceInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/css/**",
                    "/js/**", 
                    "/images/**",
                    "/fonts/**",
                    "/plugins/**",
                    "/dist/**",
                    "/favicon.ico"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 허용된 오리진 (도메인/서브도메인/포트 조합) - 세션 쿠키 정합성 보장
                .allowedOrigins(
                    "http://hntsolution.co.kr",
                    "http://hntsolution.co.kr:8080",
                    "http://hntsolution.co.kr:8888",
                    "https://hntsolution.co.kr",
                    "https://hntsolution.co.kr:8080",
                    "https://hntsolution.co.kr:8888",
                    "http://iot.hntsolution.co.kr",
                    "http://iot.hntsolution.co.kr:8080",
                    "http://iot.hntsolution.co.kr:8888",
                    "https://iot.hntsolution.co.kr",
                    "https://iot.hntsolution.co.kr:8080",
                    "https://iot.hntsolution.co.kr:8888",
                    "http://localhost:8080",
                    "http://localhost:8888",
                    "http://127.0.0.1:8080",
                    "http://127.0.0.1:8888"
                )
                // 허용된 HTTP 메서드 (RESTful API 지원)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                // 허용된 헤더 (세션 쿠키 관련 헤더 포함)
                .allowedHeaders(
                    "Origin", "X-Requested-With", "Content-Type", "Accept", 
                    "Authorization", "Cache-Control", "Pragma", "X-CSRF-Token",
                    "X-Requested-With", "X-Forwarded-For", "X-Real-IP",
                    "Cookie", "Set-Cookie", "JSESSIONID"  // 세션 쿠키 관련 헤더 추가
                )
                // 노출할 헤더 (클라이언트에서 접근 가능한 헤더)
                .exposedHeaders(
                    "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials",
                    "Set-Cookie", "JSESSIONID", "Access-Control-Expose-Headers"
                )
                // 자격증명 허용 (쿠키/인증 정보 포함) - 세션 쿠키 정합성 핵심
                .allowCredentials(true)
                // preflight 요청 캐시 시간 (1시간) - 성능 최적화
                .maxAge(3600);
    }
}