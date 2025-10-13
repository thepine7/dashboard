package com.andrew.hnt.api.service;

import com.andrew.hnt.api.util.PermissionValidationManager;
import com.andrew.hnt.api.util.PermissionValidationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 통합 권한 검증 서비스
 * 모든 컨트롤러에서 일관된 권한 검증을 제공
 */
@Service
public class UnifiedPermissionValidationService {
    
    @Autowired
    private PermissionValidationManager permissionValidationManager;
    
    /**
     * 권한 검증 수행
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @param requiredPermission 필요한 권한
     * @return 검증 결과
     */
    public PermissionValidationStrategy.ValidationResult validatePermission(
            HttpSession session, 
            HttpServletRequest request, 
            String requiredPermission) {
        
        PermissionValidationStrategy.PermissionContext context = 
            new PermissionValidationStrategy.PermissionContext(session, request, requiredPermission);
        
        return permissionValidationManager.validatePermission(context);
    }
    
    /**
     * 리소스 기반 권한 검증 수행
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @param requiredPermission 필요한 권한
     * @param resourceId 리소스 ID
     * @param action 액션
     * @return 검증 결과
     */
    public PermissionValidationStrategy.ValidationResult validatePermission(
            HttpSession session, 
            HttpServletRequest request, 
            String requiredPermission,
            String resourceId,
            String action) {
        
        PermissionValidationStrategy.PermissionContext context = 
            new PermissionValidationStrategy.PermissionContext(session, request, requiredPermission, 
                                                             resourceId, action, null, 0, System.currentTimeMillis());
        
        return permissionValidationManager.validatePermission(context);
    }
    
    /**
     * 컨텍스트 데이터를 포함한 권한 검증 수행
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @param requiredPermission 필요한 권한
     * @param resourceId 리소스 ID
     * @param action 액션
     * @param contextData 컨텍스트 데이터
     * @return 검증 결과
     */
    public PermissionValidationStrategy.ValidationResult validatePermission(
            HttpSession session, 
            HttpServletRequest request, 
            String requiredPermission,
            String resourceId,
            String action,
            Map<String, Object> contextData) {
        
        PermissionValidationStrategy.PermissionContext context = 
            new PermissionValidationStrategy.PermissionContext(session, request, requiredPermission, 
                                                             resourceId, action, contextData, 0, System.currentTimeMillis());
        
        return permissionValidationManager.validatePermission(context);
    }
    
    /**
     * 권한 검증 가능 여부 확인
     * @param session HTTP 세션
     * @param request HTTP 요청
     * @param requiredPermission 필요한 권한
     * @return 검증 가능 여부
     */
    public boolean canValidate(HttpSession session, HttpServletRequest request, String requiredPermission) {
        PermissionValidationStrategy.PermissionContext context = 
            new PermissionValidationStrategy.PermissionContext(session, request, requiredPermission);
        
        return permissionValidationManager.canValidate(context);
    }
    
    /**
     * 권한 검증 통계 조회
     * @return 검증 통계
     */
    public Map<String, Object> getValidationStats() {
        return permissionValidationManager.getValidationStats();
    }
    
    /**
     * 검증 시도 기록 조회
     * @param validationId 검증 ID
     * @return 검증 시도 기록
     */
    public PermissionValidationManager.ValidationAttempt getValidationAttempt(String validationId) {
        return permissionValidationManager.getValidationAttempt(validationId);
    }
    
    /**
     * 검증 시도 기록 초기화
     * @param validationId 검증 ID
     */
    public void clearValidationAttempt(String validationId) {
        permissionValidationManager.clearValidationAttempt(validationId);
    }
    
    /**
     * 모든 검증 시도 기록 초기화
     */
    public void clearAllValidationAttempts() {
        permissionValidationManager.clearAllValidationAttempts();
    }
}
