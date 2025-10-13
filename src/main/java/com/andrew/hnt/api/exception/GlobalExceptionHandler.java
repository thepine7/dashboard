package com.andrew.hnt.api.exception;

import com.andrew.hnt.api.util.UnifiedErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * 전역 예외 처리 핸들러
 * 모든 컨트롤러에서 발생하는 예외를 중앙에서 처리
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @Autowired
    private UnifiedErrorHandler unifiedErrorHandler;
    
    /**
     * 비즈니스 예외 처리
     * @param ex BusinessException
     * @param request WebRequest
     * @return ResponseEntity<Map<String, Object>>
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(
            BusinessException ex, 
            WebRequest request) {
        
        logger.warn("비즈니스 예외 발생: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("resultCode", ex.getErrorCode());
        errorResponse.put("resultMessage", ex.getMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getDescription(false));
        
        // 에러 로깅
        unifiedErrorHandler.logError("비즈니스 예외", ex);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 데이터베이스 예외 처리
     * @param ex DatabaseException
     * @param request WebRequest
     * @return ResponseEntity<Map<String, Object>>
     */
    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseException(
            DatabaseException ex, 
            WebRequest request) {
        
        logger.error("데이터베이스 예외 발생: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("resultCode", ex.getErrorCode());
        errorResponse.put("resultMessage", "데이터베이스 오류가 발생했습니다.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getDescription(false));
        
        // 에러 로깅
        unifiedErrorHandler.logError("데이터베이스 예외", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * MQTT 예외 처리
     * @param ex MqttException
     * @param request WebRequest
     * @return ResponseEntity<Map<String, Object>>
     */
    @ExceptionHandler(MqttException.class)
    public ResponseEntity<Map<String, Object>> handleMqttException(
            MqttException ex, 
            WebRequest request) {
        
        logger.error("MQTT 예외 발생: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("resultCode", ex.getErrorCode());
        errorResponse.put("resultMessage", "MQTT 통신 오류가 발생했습니다.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getDescription(false));
        
        // 에러 로깅
        unifiedErrorHandler.logError("MQTT 예외", ex);
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }
    
    /**
     * 세션 예외 처리
     * @param ex SessionException
     * @param request WebRequest
     * @return ResponseEntity<Map<String, Object>>
     */
    @ExceptionHandler(SessionException.class)
    public ResponseEntity<Map<String, Object>> handleSessionException(
            SessionException ex, 
            WebRequest request) {
        
        logger.warn("세션 예외 발생: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("resultCode", ex.getErrorCode());
        errorResponse.put("resultMessage", ex.getMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getDescription(false));
        errorResponse.put("redirectUrl", "/login/login");
        
        // 에러 로깅
        unifiedErrorHandler.logError("세션 예외", ex);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * 권한 예외 처리
     * @param ex PermissionException
     * @param request WebRequest
     * @return ResponseEntity<Map<String, Object>>
     */
    @ExceptionHandler(PermissionException.class)
    public ResponseEntity<Map<String, Object>> handlePermissionException(
            PermissionException ex, 
            WebRequest request) {
        
        logger.warn("권한 예외 발생: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("resultCode", ex.getErrorCode());
        errorResponse.put("resultMessage", ex.getMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getDescription(false));
        
        // 에러 로깅
        unifiedErrorHandler.logError("권한 예외", ex);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * 유효성 검사 예외 처리
     * @param ex ValidationException
     * @param request WebRequest
     * @return ResponseEntity<Map<String, Object>>
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            ValidationException ex, 
            WebRequest request) {
        
        logger.warn("유효성 검사 예외 발생: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("resultCode", ex.getErrorCode());
        errorResponse.put("resultMessage", ex.getMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getDescription(false));
        errorResponse.put("validationErrors", ex.getValidationErrors());
        
        // 에러 로깅
        unifiedErrorHandler.logError("유효성 검사 예외", ex);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * 일반 예외 처리 (RuntimeException, Exception)
     * @param ex Exception
     * @param request WebRequest
     * @return ResponseEntity<Map<String, Object>>
     */
    @ExceptionHandler({RuntimeException.class, Exception.class})
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, 
            WebRequest request) {
        
        logger.error("일반 예외 발생: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("resultCode", "500");
        errorResponse.put("resultMessage", "시스템 오류가 발생했습니다.");
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("path", request.getDescription(false));
        
        // 에러 로깅
        unifiedErrorHandler.logError("일반 예외", ex);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
}