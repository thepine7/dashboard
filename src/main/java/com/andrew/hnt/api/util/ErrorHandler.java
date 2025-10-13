package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 공통 에러 처리 클래스
 * HnT Sensor API 프로젝트
 * 
 * 주요 기능:
 * - 통일된 에러 응답 형식
 * - 에러 로깅 표준화
 * - 사용자 친화적 에러 메시지
 */
@Component
public class ErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
    
    // 에러 코드 상수
    public static final String ERROR_CODE_SUCCESS = "200";
    public static final String ERROR_CODE_BAD_REQUEST = "400";
    public static final String ERROR_CODE_UNAUTHORIZED = "401";
    public static final String ERROR_CODE_FORBIDDEN = "403";
    public static final String ERROR_CODE_NOT_FOUND = "404";
    public static final String ERROR_CODE_INTERNAL_SERVER_ERROR = "500";
    public static final String ERROR_CODE_MQTT_CONNECTION_FAILED = "1001";
    public static final String ERROR_CODE_DATABASE_ERROR = "1002";
    public static final String ERROR_CODE_VALIDATION_ERROR = "1003";
    public static final String ERROR_CODE_SENSOR_NOT_FOUND = "1004";
    public static final String ERROR_CODE_USER_PERMISSION_DENIED = "1005";
    
    // 에러 메시지 상수
    public static final String MESSAGE_SUCCESS = "성공";
    public static final String MESSAGE_BAD_REQUEST = "잘못된 요청입니다.";
    public static final String MESSAGE_UNAUTHORIZED = "인증이 필요합니다.";
    public static final String MESSAGE_FORBIDDEN = "접근 권한이 없습니다.";
    public static final String MESSAGE_NOT_FOUND = "요청한 리소스를 찾을 수 없습니다.";
    public static final String MESSAGE_INTERNAL_SERVER_ERROR = "서버 내부 오류가 발생했습니다.";
    public static final String MESSAGE_MQTT_CONNECTION_FAILED = "MQTT 연결에 실패했습니다.";
    public static final String MESSAGE_DATABASE_ERROR = "데이터베이스 오류가 발생했습니다.";
    public static final String MESSAGE_VALIDATION_ERROR = "입력값 검증에 실패했습니다.";
    public static final String MESSAGE_SENSOR_NOT_FOUND = "센서를 찾을 수 없습니다.";
    public static final String MESSAGE_USER_PERMISSION_DENIED = "사용자 권한이 부족합니다.";
    
    /**
     * 성공 응답 생성
     */
    public Map<String, Object> createSuccessResponse() {
        return createSuccessResponse(MESSAGE_SUCCESS);
    }
    
    /**
     * 성공 응답 생성 (사용자 정의 메시지)
     */
    public Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("resultCode", ERROR_CODE_SUCCESS);
        response.put("resultMessage", message);
        return response;
    }
    
    /**
     * 성공 응답 생성 (데이터 포함)
     */
    public Map<String, Object> createSuccessResponse(String message, Object data) {
        Map<String, Object> response = createSuccessResponse(message);
        response.put("data", data);
        return response;
    }
    
    /**
     * 에러 응답 생성
     */
    public Map<String, Object> createErrorResponse(String errorCode, String errorMessage) {
        Map<String, Object> response = new HashMap<>();
        response.put("resultCode", errorCode);
        response.put("resultMessage", errorMessage);
        return response;
    }
    
    /**
     * 에러 응답 생성 (예외 포함)
     */
    public Map<String, Object> createErrorResponse(String errorCode, String errorMessage, Exception e) {
        // 에러 로깅
        logError(errorCode, errorMessage, e);
        
        Map<String, Object> response = new HashMap<>();
        response.put("resultCode", errorCode);
        response.put("resultMessage", errorMessage);
        return response;
    }
    
    /**
     * 잘못된 요청 에러 응답
     */
    public Map<String, Object> createBadRequestResponse() {
        return createErrorResponse(ERROR_CODE_BAD_REQUEST, MESSAGE_BAD_REQUEST);
    }
    
    /**
     * 잘못된 요청 에러 응답 (사용자 정의 메시지)
     */
    public Map<String, Object> createBadRequestResponse(String message) {
        return createErrorResponse(ERROR_CODE_BAD_REQUEST, message);
    }
    
    /**
     * 인증 에러 응답
     */
    public Map<String, Object> createUnauthorizedResponse() {
        return createErrorResponse(ERROR_CODE_UNAUTHORIZED, MESSAGE_UNAUTHORIZED);
    }
    
    /**
     * 권한 에러 응답
     */
    public Map<String, Object> createForbiddenResponse() {
        return createErrorResponse(ERROR_CODE_FORBIDDEN, MESSAGE_FORBIDDEN);
    }
    
    /**
     * 권한 에러 응답 (사용자 정의 메시지)
     */
    public Map<String, Object> createForbiddenResponse(String message) {
        return createErrorResponse(ERROR_CODE_FORBIDDEN, message);
    }
    
    /**
     * 찾을 수 없음 에러 응답
     */
    public Map<String, Object> createNotFoundResponse() {
        return createErrorResponse(ERROR_CODE_NOT_FOUND, MESSAGE_NOT_FOUND);
    }
    
    /**
     * 찾을 수 없음 에러 응답 (사용자 정의 메시지)
     */
    public Map<String, Object> createNotFoundResponse(String message) {
        return createErrorResponse(ERROR_CODE_NOT_FOUND, message);
    }
    
    /**
     * 서버 내부 에러 응답
     */
    public Map<String, Object> createInternalServerErrorResponse() {
        return createErrorResponse(ERROR_CODE_INTERNAL_SERVER_ERROR, MESSAGE_INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 서버 내부 에러 응답 (예외 포함)
     */
    public Map<String, Object> createInternalServerErrorResponse(Exception e) {
        return createErrorResponse(ERROR_CODE_INTERNAL_SERVER_ERROR, MESSAGE_INTERNAL_SERVER_ERROR, e);
    }
    
    /**
     * MQTT 연결 실패 에러 응답
     */
    public Map<String, Object> createMqttConnectionFailedResponse() {
        return createErrorResponse(ERROR_CODE_MQTT_CONNECTION_FAILED, MESSAGE_MQTT_CONNECTION_FAILED);
    }
    
    /**
     * 데이터베이스 에러 응답
     */
    public Map<String, Object> createDatabaseErrorResponse() {
        return createErrorResponse(ERROR_CODE_DATABASE_ERROR, MESSAGE_DATABASE_ERROR);
    }
    
    /**
     * 데이터베이스 에러 응답 (예외 포함)
     */
    public Map<String, Object> createDatabaseErrorResponse(Exception e) {
        return createErrorResponse(ERROR_CODE_DATABASE_ERROR, MESSAGE_DATABASE_ERROR, e);
    }
    
    /**
     * 검증 에러 응답
     */
    public Map<String, Object> createValidationErrorResponse(String message) {
        return createErrorResponse(ERROR_CODE_VALIDATION_ERROR, message);
    }
    
    /**
     * 센서 찾을 수 없음 에러 응답
     */
    public Map<String, Object> createSensorNotFoundResponse() {
        return createErrorResponse(ERROR_CODE_SENSOR_NOT_FOUND, MESSAGE_SENSOR_NOT_FOUND);
    }
    
    /**
     * 사용자 권한 에러 응답
     */
    public Map<String, Object> createUserPermissionDeniedResponse() {
        return createErrorResponse(ERROR_CODE_USER_PERMISSION_DENIED, MESSAGE_USER_PERMISSION_DENIED);
    }
    
    /**
     * 사용자 권한 에러 응답 (사용자 정의 메시지)
     */
    public Map<String, Object> createUserPermissionDeniedResponse(String message) {
        return createErrorResponse(ERROR_CODE_USER_PERMISSION_DENIED, message);
    }
    
    /**
     * 에러 로깅
     */
    private void logError(String errorCode, String errorMessage, Exception e) {
        logger.error("에러 발생 - 코드: {}, 메시지: {}", errorCode, errorMessage, e);
    }
    
    /**
     * 에러 로깅 (예외 없음)
     */
    private void logError(String errorCode, String errorMessage) {
        logger.error("에러 발생 - 코드: {}, 메시지: {}", errorCode, errorMessage);
    }
    
    /**
     * 경고 로깅
     */
    public void logWarning(String message) {
        logger.warn(message);
    }
    
    /**
     * 정보 로깅
     */
    public void logInfo(String message) {
        logger.info(message);
    }
    
    /**
     * 디버그 로깅
     */
    public void logDebug(String message) {
        logger.debug(message);
    }
}
