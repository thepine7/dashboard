package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 통합 에러 처리 핸들러
 * HnT Sensor API 프로젝트 전용
 * 
 * 모든 컨트롤러에서 일관된 에러 처리를 위한 통합 시스템
 * 
 * 주요 기능:
 * - 통일된 에러 응답 형식
 * - 에러 로깅 표준화
 * - 사용자 친화적 에러 메시지
 * - 에러 타입별 분류 처리
 */
@Component
public class UnifiedErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(UnifiedErrorHandler.class);
    
    // 에러 코드 상수
    public static final String SUCCESS_CODE = "200";
    public static final String BAD_REQUEST_CODE = "400";
    public static final String UNAUTHORIZED_CODE = "401";
    public static final String FORBIDDEN_CODE = "403";
    public static final String NOT_FOUND_CODE = "404";
    public static final String INTERNAL_SERVER_ERROR_CODE = "500";
    public static final String DATABASE_ERROR_CODE = "503";
    public static final String MQTT_ERROR_CODE = "504";
    public static final String VALIDATION_ERROR_CODE = "400";
    public static final String BUSINESS_LOGIC_ERROR_CODE = "409";
    
    // 에러 메시지 상수
    public static final String SUCCESS_MESSAGE = "성공";
    public static final String BAD_REQUEST_MESSAGE = "잘못된 요청입니다.";
    public static final String UNAUTHORIZED_MESSAGE = "인증이 필요합니다.";
    public static final String FORBIDDEN_MESSAGE = "접근 권한이 없습니다.";
    public static final String NOT_FOUND_MESSAGE = "요청한 리소스를 찾을 수 없습니다.";
    public static final String INTERNAL_SERVER_ERROR_MESSAGE = "서버 내부 오류가 발생했습니다.";
    public static final String DATABASE_ERROR_MESSAGE = "데이터베이스 오류가 발생했습니다.";
    public static final String MQTT_ERROR_MESSAGE = "MQTT 통신 오류가 발생했습니다.";
    public static final String VALIDATION_ERROR_MESSAGE = "입력 데이터가 유효하지 않습니다.";
    public static final String BUSINESS_LOGIC_ERROR_MESSAGE = "비즈니스 로직 오류가 발생했습니다.";
    
    /**
     * 성공 응답 생성
     */
    public Map<String, Object> createSuccessResponse() {
        return createSuccessResponse(SUCCESS_MESSAGE);
    }
    
    /**
     * 성공 응답 생성 (사용자 정의 메시지)
     */
    public Map<String, Object> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("resultCode", SUCCESS_CODE);
        response.put("resultMessage", message);
        response.put("timestamp", System.currentTimeMillis());
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
     * 잘못된 요청 에러 응답 생성
     */
    public Map<String, Object> createBadRequestResponse() {
        return createErrorResponse(BAD_REQUEST_CODE, BAD_REQUEST_MESSAGE);
    }
    
    /**
     * 잘못된 요청 에러 응답 생성 (사용자 정의 메시지)
     */
    public Map<String, Object> createBadRequestResponse(String message) {
        return createErrorResponse(BAD_REQUEST_CODE, message);
    }
    
    /**
     * 인증 에러 응답 생성
     */
    public Map<String, Object> createUnauthorizedResponse() {
        return createErrorResponse(UNAUTHORIZED_CODE, UNAUTHORIZED_MESSAGE);
    }
    
    /**
     * 인증 에러 응답 생성 (사용자 정의 메시지)
     */
    public Map<String, Object> createUnauthorizedResponse(String message) {
        return createErrorResponse(UNAUTHORIZED_CODE, message);
    }
    
    /**
     * 권한 에러 응답 생성
     */
    public Map<String, Object> createForbiddenResponse() {
        return createErrorResponse(FORBIDDEN_CODE, FORBIDDEN_MESSAGE);
    }
    
    /**
     * 권한 에러 응답 생성 (사용자 정의 메시지)
     */
    public Map<String, Object> createForbiddenResponse(String message) {
        return createErrorResponse(FORBIDDEN_CODE, message);
    }
    
    /**
     * 리소스 없음 에러 응답 생성
     */
    public Map<String, Object> createNotFoundResponse() {
        return createErrorResponse(NOT_FOUND_CODE, NOT_FOUND_MESSAGE);
    }
    
    /**
     * 리소스 없음 에러 응답 생성 (사용자 정의 메시지)
     */
    public Map<String, Object> createNotFoundResponse(String message) {
        return createErrorResponse(NOT_FOUND_CODE, message);
    }
    
    /**
     * 서버 내부 에러 응답 생성
     */
    public Map<String, Object> createInternalServerErrorResponse() {
        return createErrorResponse(INTERNAL_SERVER_ERROR_CODE, INTERNAL_SERVER_ERROR_MESSAGE);
    }
    
    /**
     * 서버 내부 에러 응답 생성 (예외 포함)
     */
    public Map<String, Object> createInternalServerErrorResponse(Exception e) {
        logError("서버 내부 오류", e);
        return createErrorResponse(INTERNAL_SERVER_ERROR_CODE, INTERNAL_SERVER_ERROR_MESSAGE);
    }
    
    /**
     * 서버 내부 에러 응답 생성 (사용자 정의 메시지)
     */
    public Map<String, Object> createInternalServerErrorResponse(String message) {
        return createErrorResponse(INTERNAL_SERVER_ERROR_CODE, message);
    }
    
    /**
     * 데이터베이스 에러 응답 생성
     */
    public Map<String, Object> createDatabaseErrorResponse() {
        return createErrorResponse(DATABASE_ERROR_CODE, DATABASE_ERROR_MESSAGE);
    }
    
    /**
     * 데이터베이스 에러 응답 생성 (예외 포함)
     */
    public Map<String, Object> createDatabaseErrorResponse(Exception e) {
        logError("데이터베이스 오류", e);
        return createErrorResponse(DATABASE_ERROR_CODE, DATABASE_ERROR_MESSAGE);
    }
    
    /**
     * MQTT 에러 응답 생성
     */
    public Map<String, Object> createMqttErrorResponse() {
        return createErrorResponse(MQTT_ERROR_CODE, MQTT_ERROR_MESSAGE);
    }
    
    /**
     * MQTT 에러 응답 생성 (예외 포함)
     */
    public Map<String, Object> createMqttErrorResponse(Exception e) {
        logError("MQTT 통신 오류", e);
        return createErrorResponse(MQTT_ERROR_CODE, MQTT_ERROR_MESSAGE);
    }
    
    /**
     * 검증 에러 응답 생성
     */
    public Map<String, Object> createValidationErrorResponse() {
        return createErrorResponse(VALIDATION_ERROR_CODE, VALIDATION_ERROR_MESSAGE);
    }
    
    /**
     * 검증 에러 응답 생성 (사용자 정의 메시지)
     */
    public Map<String, Object> createValidationErrorResponse(String message) {
        return createErrorResponse(VALIDATION_ERROR_CODE, message);
    }
    
    /**
     * 비즈니스 로직 에러 응답 생성
     */
    public Map<String, Object> createBusinessLogicErrorResponse() {
        return createErrorResponse(BUSINESS_LOGIC_ERROR_CODE, BUSINESS_LOGIC_ERROR_MESSAGE);
    }
    
    /**
     * 비즈니스 로직 에러 응답 생성 (사용자 정의 메시지)
     */
    public Map<String, Object> createBusinessLogicErrorResponse(String message) {
        return createErrorResponse(BUSINESS_LOGIC_ERROR_CODE, message);
    }
    
    /**
     * 일반 에러 응답 생성
     */
    public Map<String, Object> createErrorResponse(String code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("resultCode", code);
        response.put("resultMessage", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }
    
    /**
     * 예외 타입별 에러 응답 생성
     */
    public Map<String, Object> createErrorResponseByException(Exception e) {
        if (e instanceof IllegalArgumentException) {
            return createValidationErrorResponse(e.getMessage());
        } else if (e instanceof SecurityException) {
            return createForbiddenResponse(e.getMessage());
        } else if (e instanceof RuntimeException) {
            return createBusinessLogicErrorResponse(e.getMessage());
        } else {
            return createInternalServerErrorResponse(e);
        }
    }
    
    /**
     * 에러 로깅 (표준화)
     */
    public void logError(String context, Exception e) {
        logger.error("[{}] 오류 발생: {}", context, e.getMessage(), e);
    }
    
    /**
     * 에러 로깅 (사용자 정의 메시지)
     */
    public void logError(String context, String message, Exception e) {
        logger.error("[{}] {}: {}", context, message, e.getMessage(), e);
    }
    
    /**
     * 경고 로깅
     */
    public void logWarning(String context, String message) {
        logger.warn("[{}] 경고: {}", context, message);
    }
    
    /**
     * 정보 로깅
     */
    public void logInfo(String context, String message) {
        logger.info("[{}] 정보: {}", context, message);
    }
    
    /**
     * 디버그 로깅
     */
    public void logDebug(String context, String message) {
        logger.debug("[{}] 디버그: {}", context, message);
    }
}
