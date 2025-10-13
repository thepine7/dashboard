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
 * 관리자 권한 검증 전략
 * 관리자 전용 기능에 대한 권한 검증을 수행
 */
@Component
public class AdminPermissionValidationStrategy implements PermissionValidationStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminPermissionValidationStrategy.class);
    
    @Autowired
    private SessionManagementService sessionManagementService;
    
    @Override
    public ValidationResult validatePermission(PermissionContext context) {
        logger.info("관리자 권한 검증 시작 - 권한: {}", context.getRequiredPermission());
        
        try {
            HttpSession session = context.getSession();
            String requiredPermission = context.getRequiredPermission();
            
            // 1. 세션 유효성 검사
            if (!sessionManagementService.isValidSession(session)) {
                logger.warn("세션이 유효하지 않음");
                return new ValidationResult(false, "세션이 유효하지 않습니다.", "/login/login");
            }
            
            // 2. 관리자 권한 확인
            if (!sessionManagementService.isAdmin(session)) {
                logger.warn("관리자 권한이 없음 - 사용자: {}", session.getAttribute("userId"));
                return new ValidationResult(false, "관리자 권한이 필요합니다.", "/main/main");
            }
            
            // 3. 특정 관리자 기능 권한 확인
            if (isSpecificAdminFunction(requiredPermission)) {
                if (!hasSpecificAdminPermission(session, requiredPermission)) {
                    logger.warn("특정 관리자 권한이 없음 - 사용자: {}, 권한: {}", 
                               session.getAttribute("userId"), requiredPermission);
                    return new ValidationResult(false, "해당 관리자 권한이 없습니다.", "/main/main");
                }
            }
            
            // 4. 추가 정보 수집
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("userId", session.getAttribute("userId"));
            additionalData.put("userGrade", session.getAttribute("userGrade"));
            additionalData.put("userNm", session.getAttribute("userNm"));
            additionalData.put("isAdmin", true);
            additionalData.put("validationTime", System.currentTimeMillis());
            
            logger.info("관리자 권한 검증 성공 - 사용자: {}, 권한: {}", 
                       session.getAttribute("userId"), requiredPermission);
            
            return new ValidationResult(true, "관리자 권한 검증 성공", null, additionalData);
            
        } catch (Exception e) {
            logger.error("관리자 권한 검증 중 예외 발생", e);
            return new ValidationResult(false, "관리자 권한 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canValidate(PermissionContext context) {
        // 관리자 권한 검증은 A 등급 권한에 대해서만 수행
        return "A".equals(context.getRequiredPermission()) || 
               isAdminFunction(context.getRequiredPermission());
    }
    
    @Override
    public int getPriority() {
        return 2; // 중간 우선순위
    }
    
    @Override
    public PermissionType getPermissionType() {
        return PermissionType.ADMIN_ONLY;
    }
    
    /**
     * 특정 관리자 기능인지 확인
     * @param permission 권한
     * @return 관리자 기능 여부
     */
    private boolean isSpecificAdminFunction(String permission) {
        return "USER_MANAGEMENT".equals(permission) || 
               "SYSTEM_MANAGEMENT".equals(permission) ||
               "ADMIN_SETTINGS".equals(permission);
    }
    
    /**
     * 관리자 기능인지 확인
     * @param permission 권한
     * @return 관리자 기능 여부
     */
    private boolean isAdminFunction(String permission) {
        return "A".equals(permission) || 
               isSpecificAdminFunction(permission);
    }
    
    /**
     * 특정 관리자 권한 확인
     * @param session 세션
     * @param permission 권한
     * @return 권한 여부
     */
    private boolean hasSpecificAdminPermission(HttpSession session, String permission) {
        // 실제 구현에서는 더 세밀한 권한 체크를 수행
        // 예: 사용자 관리 권한, 시스템 설정 권한 등
        
        switch (permission) {
            case "USER_MANAGEMENT":
                return sessionManagementService.canManageUsers(session);
            case "SYSTEM_MANAGEMENT":
                return sessionManagementService.isAdmin(session); // A 등급만 가능
            case "ADMIN_SETTINGS":
                return sessionManagementService.isAdmin(session); // A 등급만 가능
            default:
                return true; // 기본적으로 A 등급이면 모든 권한 허용
        }
    }
}
