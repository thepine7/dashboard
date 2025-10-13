package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * 예외 처리 공통 유틸리티
 * 모든 서비스에서 일관된 예외 처리를 위한 유틸리티
 */
public class ExceptionUtil {
    
    /**
     * 예외 타입별 처리
     */
    public enum ExceptionType {
        VALIDATION_ERROR("400", "입력 데이터가 유효하지 않습니다."),
        AUTHENTICATION_ERROR("401", "인증이 필요합니다."),
        AUTHORIZATION_ERROR("403", "접근 권한이 없습니다."),
        NOT_FOUND_ERROR("404", "요청한 리소스를 찾을 수 없습니다."),
        BUSINESS_LOGIC_ERROR("409", "비즈니스 로직 오류가 발생했습니다."),
        INTERNAL_SERVER_ERROR("500", "서버 내부 오류가 발생했습니다."),
        DATABASE_ERROR("503", "데이터베이스 오류가 발생했습니다."),
        MQTT_ERROR("504", "MQTT 통신 오류가 발생했습니다."),
        UNKNOWN_ERROR("999", "알 수 없는 오류가 발생했습니다.");
        
        private final String code;
        private final String message;
        
        ExceptionType(String code, String message) {
            this.code = code;
            this.message = message;
        }
        
        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
    
    /**
     * 예외를 분석하여 적절한 응답 생성
     * @param logger Logger 인스턴스
     * @param e 예외 객체
     * @param operationName 작업 이름
     * @return ResponseEntity<Map<String, Object>>
     */
    public static ResponseEntity<Map<String, Object>> handleException(Logger logger, Exception e, String operationName) {
        ExceptionType exceptionType = analyzeException(e);
        
        // 로그 레벨에 따른 로깅
        switch (exceptionType) {
            case VALIDATION_ERROR:
            case AUTHENTICATION_ERROR:
            case AUTHORIZATION_ERROR:
            case NOT_FOUND_ERROR:
                StringUtil.logWarn(logger, "{} - {}: {}", operationName, exceptionType.getMessage(), e.getMessage());
                break;
            case BUSINESS_LOGIC_ERROR:
                StringUtil.logWarn(logger, "{} - {}: {}", operationName, exceptionType.getMessage(), e.getMessage());
                break;
            case INTERNAL_SERVER_ERROR:
            case DATABASE_ERROR:
            case MQTT_ERROR:
            case UNKNOWN_ERROR:
            default:
                StringUtil.logError(logger, operationName + " - " + exceptionType.getMessage() + ": " + e.getMessage(), e);
                break;
        }
        
        return createErrorResponse(exceptionType, operationName + " 처리 중 오류가 발생했습니다.");
    }
    
    /**
     * 예외를 분석하여 ExceptionType 반환
     * @param e 예외 객체
     * @return ExceptionType
     */
    private static ExceptionType analyzeException(Exception e) {
        if (e == null) {
            return ExceptionType.UNKNOWN_ERROR;
        }
        
        String exceptionName = e.getClass().getSimpleName();
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        
        // IllegalArgumentException, ValidationException 등
        if (exceptionName.contains("Validation") || exceptionName.contains("IllegalArgument")) {
            return ExceptionType.VALIDATION_ERROR;
        }
        
        // AuthenticationException, SecurityException 등
        if (exceptionName.contains("Authentication") || exceptionName.contains("Security")) {
            return ExceptionType.AUTHENTICATION_ERROR;
        }
        
        // AccessDeniedException, ForbiddenException 등
        if (exceptionName.contains("AccessDenied") || exceptionName.contains("Forbidden")) {
            return ExceptionType.AUTHORIZATION_ERROR;
        }
        
        // NoSuchElementException, EntityNotFoundException 등
        if (exceptionName.contains("NoSuch") || exceptionName.contains("NotFound")) {
            return ExceptionType.NOT_FOUND_ERROR;
        }
        
        // BusinessException, ServiceException 등
        if (exceptionName.contains("Business") || exceptionName.contains("Service")) {
            return ExceptionType.BUSINESS_LOGIC_ERROR;
        }
        
        // SQLException, DataAccessException 등
        if (exceptionName.contains("SQL") || exceptionName.contains("DataAccess") || 
            exceptionName.contains("Database") || message.contains("database")) {
            return ExceptionType.DATABASE_ERROR;
        }
        
        // MQTTException, MqttException 등
        if (exceptionName.contains("Mqtt") || exceptionName.contains("MQTT") || 
            message.contains("mqtt") || message.contains("connection")) {
            return ExceptionType.MQTT_ERROR;
        }
        
        // RuntimeException, Exception 등
        if (exceptionName.contains("Runtime") || exceptionName.contains("Exception")) {
            return ExceptionType.INTERNAL_SERVER_ERROR;
        }
        
        return ExceptionType.UNKNOWN_ERROR;
    }
    
    /**
     * 에러 응답 생성
     * @param exceptionType 예외 타입
     * @param message 에러 메시지
     * @return ResponseEntity<Map<String, Object>>
     */
    private static ResponseEntity<Map<String, Object>> createErrorResponse(ExceptionType exceptionType, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("resultCode", exceptionType.getCode());
        response.put("resultMessage", message);
        response.put("success", false);
        response.put("error", true);
        response.put("errorType", exceptionType.name());
        
        HttpStatus httpStatus = getHttpStatus(exceptionType);
        return ResponseEntity.status(httpStatus).body(response);
    }
    
    /**
     * ExceptionType에 따른 HttpStatus 반환
     * @param exceptionType 예외 타입
     * @return HttpStatus
     */
    private static HttpStatus getHttpStatus(ExceptionType exceptionType) {
        switch (exceptionType) {
            case VALIDATION_ERROR:
                return HttpStatus.BAD_REQUEST;
            case AUTHENTICATION_ERROR:
                return HttpStatus.UNAUTHORIZED;
            case AUTHORIZATION_ERROR:
                return HttpStatus.FORBIDDEN;
            case NOT_FOUND_ERROR:
                return HttpStatus.NOT_FOUND;
            case BUSINESS_LOGIC_ERROR:
                return HttpStatus.CONFLICT;
            case INTERNAL_SERVER_ERROR:
                return HttpStatus.INTERNAL_SERVER_ERROR;
            case DATABASE_ERROR:
            case MQTT_ERROR:
                return HttpStatus.SERVICE_UNAVAILABLE;
            case UNKNOWN_ERROR:
            default:
                return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
    
    /**
     * 비즈니스 로직 예외 생성
     * @param message 예외 메시지
     * @return RuntimeException
     */
    public static RuntimeException createBusinessException(String message) {
        return new RuntimeException("BUSINESS_ERROR: " + message);
    }
    
    /**
     * 검증 예외 생성
     * @param message 예외 메시지
     * @return IllegalArgumentException
     */
    public static IllegalArgumentException createValidationException(String message) {
        return new IllegalArgumentException("VALIDATION_ERROR: " + message);
    }
    
    /**
     * 데이터베이스 예외 생성
     * @param message 예외 메시지
     * @return RuntimeException
     */
    public static RuntimeException createDatabaseException(String message) {
        return new RuntimeException("DATABASE_ERROR: " + message);
    }
    
    /**
     * MQTT 예외 생성
     * @param message 예외 메시지
     * @return RuntimeException
     */
    public static RuntimeException createMqttException(String message) {
        return new RuntimeException("MQTT_ERROR: " + message);
    }
    
    /**
     * 예외 메시지에서 원본 메시지 추출
     * @param exception 예외 객체
     * @return String
     */
    public static String extractOriginalMessage(Exception exception) {
        if (exception == null) {
            return "알 수 없는 오류";
        }
        
        String message = exception.getMessage();
        if (StringUtil.isEmpty(message)) {
            return exception.getClass().getSimpleName();
        }
        
        // "ERROR_TYPE: " 접두사 제거
        if (message.contains(": ")) {
            String[] parts = message.split(": ", 2);
            if (parts.length > 1) {
                return parts[1];
            }
        }
        
        return message;
    }
    
    /**
     * 예외 스택 트레이스를 문자열로 변환
     * @param exception 예외 객체
     * @return String
     */
    public static String getStackTraceAsString(Exception exception) {
        if (exception == null) {
            return "";
        }
        
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }
}
