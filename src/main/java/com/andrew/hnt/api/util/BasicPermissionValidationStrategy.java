package com.andrew.hnt.api.util;

import com.andrew.hnt.api.service.SessionManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 기본 권한 검증 전략
 * 사용자 등급 기반의 기본적인 권한 검증을 수행
 */
@Component
public class BasicPermissionValidationStrategy implements PermissionValidationStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(BasicPermissionValidationStrategy.class);
    
    @Autowired
    private SessionManagementService sessionManagementService;
    
    @Override
    public ValidationResult validatePermission(PermissionContext context) {
        logger.info("기본 권한 검증 시작 - 권한: {}", context.getRequiredPermission());
        
        try {
            HttpSession session = context.getSession();
            String requiredPermission = context.getRequiredPermission();
            
            // 1. 세션 유효성 검사
            if (!sessionManagementService.isValidSession(session)) {
                logger.warn("세션이 유효하지 않음");
                return new ValidationResult(false, "세션이 유효하지 않습니다.", "/login/login");
            }
            
            // 2. 사용자 등급 확인
            String userGrade = (String) session.getAttribute("userGrade");
            if (userGrade == null || userGrade.isEmpty()) {
                logger.warn("사용자 등급이 없음");
                return new ValidationResult(false, "사용자 등급이 없습니다.", "/login/login");
            }
            
            // 3. 권한 검증
            boolean hasPermission = sessionManagementService.hasPermission(session, requiredPermission);
            
            if (hasPermission) {
                // 4. 추가 정보 수집
                Map<String, Object> additionalData = new HashMap<>();
                additionalData.put("userId", session.getAttribute("userId"));
                additionalData.put("userGrade", userGrade);
                additionalData.put("userNm", session.getAttribute("userNm"));
                additionalData.put("validationTime", System.currentTimeMillis());
                
                logger.info("기본 권한 검증 성공 - 사용자: {}, 등급: {}, 권한: {}", 
                           session.getAttribute("userId"), userGrade, requiredPermission);
                
                return new ValidationResult(true, "권한 검증 성공", null, additionalData);
            } else {
                logger.warn("권한 검증 실패 - 사용자: {}, 등급: {}, 필요한 권한: {}", 
                           session.getAttribute("userId"), userGrade, requiredPermission);
                
                return new ValidationResult(false, "권한이 없습니다.", "/main/main");
            }
            
        } catch (Exception e) {
            logger.error("기본 권한 검증 중 예외 발생", e);
            return new ValidationResult(false, "권한 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canValidate(PermissionContext context) {
        // 기본 권한 검증은 모든 권한에 대해 수행 가능
        return context.getRequiredPermission() != null && !context.getRequiredPermission().isEmpty();
    }
    
    @Override
    public int getPriority() {
        return 1; // 높은 우선순위
    }
    
    @Override
    public PermissionType getPermissionType() {
        return PermissionType.PAGE_ACCESS;
    }
}
