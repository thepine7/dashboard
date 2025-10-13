package com.andrew.hnt.api.util;

import java.util.HashMap;
import java.util.Map;

/**
 * API 응답 표준화 유틸리티
 * 모든 컨트롤러와 서비스에서 일관된 응답 형식을 제공
 */
public class ResponseUtil {
    
    // 성공 응답 코드
    public static final String SUCCESS_CODE = "200";
    public static final String SUCCESS_MESSAGE = "요청이 성공적으로 처리되었습니다.";
    
    // 에러 응답 코드
    public static final String BAD_REQUEST_CODE = "400";
    public static final String UNAUTHORIZED_CODE = "401";
    public static final String FORBIDDEN_CODE = "403";
    public static final String NOT_FOUND_CODE = "404";
    public static final String INTERNAL_ERROR_CODE = "500";
    public static final String SERVICE_UNAVAILABLE_CODE = "503";
    
    // 기본 에러 메시지
    public static final String BAD_REQUEST_MESSAGE = "잘못된 요청입니다.";
    public static final String UNAUTHORIZED_MESSAGE = "인증이 필요합니다.";
    public static final String FORBIDDEN_MESSAGE = "접근 권한이 없습니다.";
    public static final String NOT_FOUND_MESSAGE = "요청한 리소스를 찾을 수 없습니다.";
    public static final String INTERNAL_ERROR_MESSAGE = "서버 내부 오류가 발생했습니다.";
    public static final String SERVICE_UNAVAILABLE_MESSAGE = "서비스를 사용할 수 없습니다.";
    
    /**
     * 성공 응답 생성
     * @param message 성공 메시지
     * @return Map<String, Object>
     */
    public static Map<String, Object> success(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("resultCode", SUCCESS_CODE);
        response.put("resultMessage", message != null ? message : SUCCESS_MESSAGE);
        return response;
    }
    
    /**
     * 성공 응답 생성 (데이터 포함)
     * @param message 성공 메시지
     * @param data 응답 데이터
     * @return Map<String, Object>
     */
    public static Map<String, Object> success(String message, Object data) {
        Map<String, Object> response = success(message);
        response.put("data", data);
        return response;
    }
    
    /**
     * 에러 응답 생성
     * @param code 에러 코드
     * @param message 에러 메시지
     * @return Map<String, Object>
     */
    public static Map<String, Object> error(String code, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("resultCode", code);
        response.put("resultMessage", message);
        return response;
    }
    
    /**
     * 400 Bad Request 응답 생성
     * @param message 에러 메시지
     * @return Map<String, Object>
     */
    public static Map<String, Object> badRequest(String message) {
        return error(BAD_REQUEST_CODE, message != null ? message : BAD_REQUEST_MESSAGE);
    }
    
    /**
     * 401 Unauthorized 응답 생성
     * @param message 에러 메시지
     * @return Map<String, Object>
     */
    public static Map<String, Object> unauthorized(String message) {
        return error(UNAUTHORIZED_CODE, message != null ? message : UNAUTHORIZED_MESSAGE);
    }
    
    /**
     * 403 Forbidden 응답 생성
     * @param message 에러 메시지
     * @return Map<String, Object>
     */
    public static Map<String, Object> forbidden(String message) {
        return error(FORBIDDEN_CODE, message != null ? message : FORBIDDEN_MESSAGE);
    }
    
    /**
     * 404 Not Found 응답 생성
     * @param message 에러 메시지
     * @return Map<String, Object>
     */
    public static Map<String, Object> notFound(String message) {
        return error(NOT_FOUND_CODE, message != null ? message : NOT_FOUND_MESSAGE);
    }
    
    /**
     * 500 Internal Server Error 응답 생성
     * @param message 에러 메시지
     * @return Map<String, Object>
     */
    public static Map<String, Object> internalError(String message) {
        return error(INTERNAL_ERROR_CODE, message != null ? message : INTERNAL_ERROR_MESSAGE);
    }
    
    /**
     * 503 Service Unavailable 응답 생성
     * @param message 에러 메시지
     * @return Map<String, Object>
     */
    public static Map<String, Object> serviceUnavailable(String message) {
        return error(SERVICE_UNAVAILABLE_CODE, message != null ? message : SERVICE_UNAVAILABLE_MESSAGE);
    }
    
    /**
     * 예외 기반 에러 응답 생성
     * @param e 예외 객체
     * @return Map<String, Object>
     */
    public static Map<String, Object> error(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : INTERNAL_ERROR_MESSAGE;
        return internalError(message);
    }
    
    /**
     * 응답이 성공인지 확인
     * @param response 응답 Map
     * @return boolean
     */
    public static boolean isSuccess(Map<String, Object> response) {
        return response != null && SUCCESS_CODE.equals(response.get("resultCode"));
    }
    
    /**
     * 응답에서 메시지 추출
     * @param response 응답 Map
     * @return String
     */
    public static String getMessage(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        return (String) response.get("resultMessage");
    }
    
    /**
     * 응답에서 데이터 추출
     * @param response 응답 Map
     * @return Object
     */
    public static Object getData(Map<String, Object> response) {
        if (response == null) {
            return null;
        }
        return response.get("data");
    }
}
