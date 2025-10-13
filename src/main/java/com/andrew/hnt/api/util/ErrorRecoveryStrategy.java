package com.andrew.hnt.api.util;

import java.util.Map;

/**
 * 에러 복구 전략 인터페이스
 * 각 에러 타입별로 적절한 복구 전략을 정의
 */
public interface ErrorRecoveryStrategy {
    
    /**
     * 에러 복구 시도
     * @param errorContext 에러 컨텍스트 정보
     * @return 복구 결과
     */
    RecoveryResult attemptRecovery(ErrorContext errorContext);
    
    /**
     * 복구 가능 여부 확인
     * @param errorContext 에러 컨텍스트 정보
     * @return 복구 가능 여부
     */
    boolean canRecover(ErrorContext errorContext);
    
    /**
     * 복구 우선순위 반환
     * @return 우선순위 (낮을수록 높은 우선순위)
     */
    int getPriority();
    
    /**
     * 에러 타입 반환
     * @return 에러 타입
     */
    ErrorType getErrorType();
    
    /**
     * 에러 복구 결과 클래스
     */
    class RecoveryResult {
        private final boolean success;
        private final String message;
        private final Map<String, Object> additionalData;
        private final long recoveryTime;
        
        public RecoveryResult(boolean success, String message) {
            this(success, message, null, System.currentTimeMillis());
        }
        
        public RecoveryResult(boolean success, String message, Map<String, Object> additionalData) {
            this(success, message, additionalData, System.currentTimeMillis());
        }
        
        public RecoveryResult(boolean success, String message, Map<String, Object> additionalData, long recoveryTime) {
            this.success = success;
            this.message = message;
            this.additionalData = additionalData;
            this.recoveryTime = recoveryTime;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, Object> getAdditionalData() { return additionalData; }
        public long getRecoveryTime() { return recoveryTime; }
    }
    
    /**
     * 에러 컨텍스트 클래스
     */
    class ErrorContext {
        private final String errorType;
        private final String errorMessage;
        private final Throwable cause;
        private final Map<String, Object> contextData;
        private final int retryCount;
        private final long firstErrorTime;
        
        public ErrorContext(String errorType, String errorMessage, Throwable cause) {
            this(errorType, errorMessage, cause, null, 0, System.currentTimeMillis());
        }
        
        public ErrorContext(String errorType, String errorMessage, Throwable cause, 
                           Map<String, Object> contextData, int retryCount, long firstErrorTime) {
            this.errorType = errorType;
            this.errorMessage = errorMessage;
            this.cause = cause;
            this.contextData = contextData;
            this.retryCount = retryCount;
            this.firstErrorTime = firstErrorTime;
        }
        
        public String getErrorType() { return errorType; }
        public String getErrorMessage() { return errorMessage; }
        public Throwable getCause() { return cause; }
        public Map<String, Object> getContextData() { return contextData; }
        public int getRetryCount() { return retryCount; }
        public long getFirstErrorTime() { return firstErrorTime; }
    }
    
    /**
     * 에러 타입 열거형
     */
    enum ErrorType {
        // 네트워크 관련 에러
        NETWORK_CONNECTION_ERROR(1, "네트워크 연결 오류"),
        MQTT_CONNECTION_ERROR(2, "MQTT 연결 오류"),
        DATABASE_CONNECTION_ERROR(3, "데이터베이스 연결 오류"),
        
        // 데이터 관련 에러
        DATA_VALIDATION_ERROR(10, "데이터 검증 오류"),
        DATA_PROCESSING_ERROR(11, "데이터 처리 오류"),
        DATA_CONSISTENCY_ERROR(12, "데이터 일관성 오류"),
        
        // 시스템 관련 에러
        SESSION_EXPIRED_ERROR(20, "세션 만료 오류"),
        PERMISSION_DENIED_ERROR(21, "권한 거부 오류"),
        RESOURCE_NOT_FOUND_ERROR(22, "리소스 없음 오류"),
        
        // 비즈니스 로직 에러
        BUSINESS_LOGIC_ERROR(30, "비즈니스 로직 오류"),
        TRANSACTION_ERROR(31, "트랜잭션 오류"),
        CONCURRENT_ACCESS_ERROR(32, "동시 접근 오류"),
        
        // 알 수 없는 에러
        UNKNOWN_ERROR(99, "알 수 없는 오류");
        
        private final int code;
        private final String description;
        
        ErrorType(int code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public int getCode() { return code; }
        public String getDescription() { return description; }
        
        public static ErrorType fromCode(int code) {
            for (ErrorType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return UNKNOWN_ERROR;
        }
    }
}
