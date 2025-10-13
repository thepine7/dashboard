package com.andrew.hnt.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 세션 보안 설정
 * 세션 타임아웃, 보안 헤더, 인터셉터 설정
 */
@Configuration
public class SessionSecurityConfig implements WebMvcConfigurer {
    
    private final SessionTimeoutInterceptor sessionTimeoutInterceptor;
    
    public SessionSecurityConfig(SessionTimeoutInterceptor sessionTimeoutInterceptor) {
        this.sessionTimeoutInterceptor = sessionTimeoutInterceptor;
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionTimeoutInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/login/**",
                    "/static/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/fonts/**",
                    "/error",
                    "/favicon.ico"
                );
    }
    
    // SessionListener는 WebConfig에서 이미 등록되어 있으므로 중복 등록 제거
}
