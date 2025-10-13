package com.andrew.hnt.api.util;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 권한 검증 전략 인터페이스
 * 각 권한 타입별로 적절한 검증 전략을 정의
 */
public interface PermissionValidationStrategy {
    
    /**
     * 권한 검증 시도
     * @param context 권한 검증 컨텍스트
     * @return 검증 결과
     */
    ValidationResult validatePermission(PermissionContext context);
    
    /**
     * 검증 가능 여부 확인
     * @param context 권한 검증 컨텍스트
     * @return 검증 가능 여부
     */
    boolean canValidate(PermissionContext context);
    
    /**
     * 검증 우선순위 반환
     * @return 우선순위 (낮을수록 높은 우선순위)
     */
    int getPriority();
    
    /**
     * 권한 타입 반환
     * @return 권한 타입
     */
    PermissionType getPermissionType();
    
    /**
     * 권한 검증 결과 클래스
     */
    class ValidationResult {
        private final boolean valid;
        private final String message;
        private final String redirectUrl;
        private final Map<String, Object> additionalData;
        private final long validationTime;
        
        public ValidationResult(boolean valid, String message) {
            this(valid, message, null, null, System.currentTimeMillis());
        }
        
        public ValidationResult(boolean valid, String message, String redirectUrl) {
            this(valid, message, redirectUrl, null, System.currentTimeMillis());
        }
        
        public ValidationResult(boolean valid, String message, String redirectUrl, Map<String, Object> additionalData) {
            this(valid, message, redirectUrl, additionalData, System.currentTimeMillis());
        }
        
        public ValidationResult(boolean valid, String message, String redirectUrl, Map<String, Object> additionalData, long validationTime) {
            this.valid = valid;
            this.message = message;
            this.redirectUrl = redirectUrl;
            this.additionalData = additionalData;
            this.validationTime = validationTime;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public String getRedirectUrl() { return redirectUrl; }
        public Map<String, Object> getAdditionalData() { return additionalData; }
        public long getValidationTime() { return validationTime; }
    }
    
    /**
     * 권한 검증 컨텍스트 클래스
     */
    class PermissionContext {
        private final HttpSession session;
        private final HttpServletRequest request;
        private final String requiredPermission;
        private final String resourceId;
        private final String action;
        private final Map<String, Object> contextData;
        private final int retryCount;
        private final long firstValidationTime;
        
        public PermissionContext(HttpSession session, HttpServletRequest request, String requiredPermission) {
            this(session, request, requiredPermission, null, null, null, 0, System.currentTimeMillis());
        }
        
        public PermissionContext(HttpSession session, HttpServletRequest request, String requiredPermission, 
                               String resourceId, String action, Map<String, Object> contextData, 
                               int retryCount, long firstValidationTime) {
            this.session = session;
            this.request = request;
            this.requiredPermission = requiredPermission;
            this.resourceId = resourceId;
            this.action = action;
            this.contextData = contextData;
            this.retryCount = retryCount;
            this.firstValidationTime = firstValidationTime;
        }
        
        public HttpSession getSession() { return session; }
        public HttpServletRequest getRequest() { return request; }
        public String getRequiredPermission() { return requiredPermission; }
        public String getResourceId() { return resourceId; }
        public String getAction() { return action; }
        public Map<String, Object> getContextData() { return contextData; }
        public int getRetryCount() { return retryCount; }
        public long getFirstValidationTime() { return firstValidationTime; }
    }
    
    /**
     * 권한 타입 열거형
     */
    enum PermissionType {
        // 기본 권한
        PAGE_ACCESS(1, "페이지 접근 권한"),
        DATA_READ(2, "데이터 읽기 권한"),
        DATA_WRITE(3, "데이터 쓰기 권한"),
        DATA_DELETE(4, "데이터 삭제 권한"),
        
        // 관리 권한
        USER_MANAGEMENT(10, "사용자 관리 권한"),
        DEVICE_MANAGEMENT(11, "장치 관리 권한"),
        SYSTEM_MANAGEMENT(12, "시스템 관리 권한"),
        
        // 특수 권한
        ADMIN_ONLY(20, "관리자 전용 권한"),
        SUB_ACCOUNT_ACCESS(21, "부계정 접근 권한"),
        SENSOR_CONTROL(22, "센서 제어 권한"),
        
        // 알 수 없는 권한
        UNKNOWN(99, "알 수 없는 권한");
        
        private final int code;
        private final String description;
        
        PermissionType(int code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public int getCode() { return code; }
        public String getDescription() { return description; }
        
        public static PermissionType fromCode(int code) {
            for (PermissionType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return UNKNOWN;
        }
    }
}
