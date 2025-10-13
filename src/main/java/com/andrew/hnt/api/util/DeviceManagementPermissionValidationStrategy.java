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
 * 장치 관리 권한 검증 전략
 * 장치 관련 기능에 대한 권한 검증을 수행
 */
@Component
public class DeviceManagementPermissionValidationStrategy implements PermissionValidationStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(DeviceManagementPermissionValidationStrategy.class);
    
    @Autowired
    private SessionManagementService sessionManagementService;
    
    @Override
    public ValidationResult validatePermission(PermissionContext context) {
        logger.info("장치 관리 권한 검증 시작 - 권한: {}", context.getRequiredPermission());
        
        try {
            HttpSession session = context.getSession();
            String requiredPermission = context.getRequiredPermission();
            String resourceId = context.getResourceId();
            String action = context.getAction();
            
            // 1. 세션 유효성 검사
            if (!sessionManagementService.isValidSession(session)) {
                logger.warn("세션이 유효하지 않음");
                return new ValidationResult(false, "세션이 유효하지 않습니다.", "/login/login");
            }
            
            // 2. 장치 관리 권한 확인
            if (!sessionManagementService.canManageDevices(session)) {
                logger.warn("장치 관리 권한이 없음 - 사용자: {}", session.getAttribute("userId"));
                return new ValidationResult(false, "장치 관리 권한이 필요합니다.", "/main/main");
            }
            
            // 3. 부계정 권한 제한 확인
            if (sessionManagementService.isSubAccount(session)) {
                logger.warn("부계정은 장치 관리 불가 - 사용자: {}", session.getAttribute("userId"));
                return new ValidationResult(false, "부계정은 장치 관리 권한이 없습니다.", "/main/main");
            }
            
            // 4. 특정 장치에 대한 권한 확인 (리소스 ID가 있는 경우)
            if (resourceId != null && !resourceId.isEmpty()) {
                if (!hasDeviceAccess(session, resourceId)) {
                    logger.warn("해당 장치에 대한 접근 권한이 없음 - 사용자: {}, 장치: {}", 
                               session.getAttribute("userId"), resourceId);
                    return new ValidationResult(false, "해당 장치에 대한 접근 권한이 없습니다.", "/main/main");
                }
            }
            
            // 5. 특정 액션에 대한 권한 확인
            if (action != null && !action.isEmpty()) {
                if (!hasActionPermission(session, action)) {
                    logger.warn("해당 액션에 대한 권한이 없음 - 사용자: {}, 액션: {}", 
                               session.getAttribute("userId"), action);
                    return new ValidationResult(false, "해당 액션에 대한 권한이 없습니다.", "/main/main");
                }
            }
            
            // 6. 추가 정보 수집
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("userId", session.getAttribute("userId"));
            additionalData.put("userGrade", session.getAttribute("userGrade"));
            additionalData.put("userNm", session.getAttribute("userNm"));
            additionalData.put("canManageDevices", true);
            additionalData.put("resourceId", resourceId);
            additionalData.put("action", action);
            additionalData.put("validationTime", System.currentTimeMillis());
            
            logger.info("장치 관리 권한 검증 성공 - 사용자: {}, 권한: {}, 장치: {}, 액션: {}", 
                       session.getAttribute("userId"), requiredPermission, resourceId, action);
            
            return new ValidationResult(true, "장치 관리 권한 검증 성공", null, additionalData);
            
        } catch (Exception e) {
            logger.error("장치 관리 권한 검증 중 예외 발생", e);
            return new ValidationResult(false, "장치 관리 권한 검증 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canValidate(PermissionContext context) {
        // 장치 관리 권한 검증은 U 등급 권한이나 장치 관련 기능에 대해서만 수행
        return "U".equals(context.getRequiredPermission()) || 
               isDeviceManagementFunction(context.getRequiredPermission()) ||
               isDeviceAction(context.getAction());
    }
    
    @Override
    public int getPriority() {
        return 3; // 낮은 우선순위
    }
    
    @Override
    public PermissionType getPermissionType() {
        return PermissionType.DEVICE_MANAGEMENT;
    }
    
    /**
     * 장치 관리 기능인지 확인
     * @param permission 권한
     * @return 장치 관리 기능 여부
     */
    private boolean isDeviceManagementFunction(String permission) {
        return "DEVICE_MANAGEMENT".equals(permission) || 
               "SENSOR_CONTROL".equals(permission) ||
               "DEVICE_DELETE".equals(permission) ||
               "DEVICE_MODIFY".equals(permission);
    }
    
    /**
     * 장치 액션인지 확인
     * @param action 액션
     * @return 장치 액션 여부
     */
    private boolean isDeviceAction(String action) {
        return "DELETE".equals(action) || 
               "MODIFY".equals(action) ||
               "CONTROL".equals(action) ||
               "CONFIGURE".equals(action);
    }
    
    /**
     * 특정 장치에 대한 접근 권한 확인
     * @param session 세션
     * @param deviceId 장치 ID
     * @return 접근 권한 여부
     */
    private boolean hasDeviceAccess(HttpSession session, String deviceId) {
        // 실제 구현에서는 데이터베이스에서 사용자가 해당 장치에 접근할 수 있는지 확인
        // 예: 장치 소유자 확인, 공유 권한 확인 등
        
        String userId = (String) session.getAttribute("userId");
        if (userId == null || userId.isEmpty()) {
            return false;
        }
        
        // 간단한 구현: 사용자 ID가 장치 ID에 포함되어 있으면 접근 허용
        // 실제로는 데이터베이스 조회가 필요
        return deviceId.contains(userId) || sessionManagementService.isAdmin(session);
    }
    
    /**
     * 특정 액션에 대한 권한 확인
     * @param session 세션
     * @param action 액션
     * @return 액션 권한 여부
     */
    private boolean hasActionPermission(HttpSession session, String action) {
        String userGrade = (String) session.getAttribute("userGrade");
        
        switch (action) {
            case "DELETE":
                // A, U 등급만 삭제 가능
                return "A".equals(userGrade) || "U".equals(userGrade);
            case "MODIFY":
                // A, U 등급만 수정 가능
                return "A".equals(userGrade) || "U".equals(userGrade);
            case "CONTROL":
                // A, U 등급만 제어 가능
                return "A".equals(userGrade) || "U".equals(userGrade);
            case "CONFIGURE":
                // A 등급만 설정 가능
                return "A".equals(userGrade);
            default:
                return true; // 기본적으로 읽기 권한은 허용
        }
    }
}
