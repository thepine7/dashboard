package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * AJAX 응답 처리 공통 유틸리티
 * 컨트롤러에서 일관된 AJAX 응답 처리를 위한 유틸리티
 */
public class AjaxResponseUtil {
    
    /**
     * 성공 응답 생성
     * @param message 성공 메시지
     * @param data 응답 데이터
     * @return ResponseEntity<Map<String, Object>>
     */
    public static ResponseEntity<Map<String, Object>> success(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("resultCode", "200");
        response.put("resultMessage", message);
        response.put("success", true);
        if (data != null) {
            response.put("data", data);
        }
        return ResponseEntity.ok(response);
    }
    
    /**
     * 성공 응답 생성 (데이터 없음)
     * @param message 성공 메시지
     * @return ResponseEntity<Map<String, Object>>
     */
    public static ResponseEntity<Map<String, Object>> success(String message) {
        return success(message, null);
    }
    
    /**
     * 에러 응답 생성
     * @param errorCode 에러 코드
     * @param message 에러 메시지
     * @param httpStatus HTTP 상태 코드
     * @return ResponseEntity<Map<String, Object>>
     */
    public static ResponseEntity<Map<String, Object>> error(String errorCode, String message, HttpStatus httpStatus) {
        Map<String, Object> response = new HashMap<>();
        response.put("resultCode", errorCode);
        response.put("resultMessage", message);
        response.put("success", false);
        response.put("error", true);
        return ResponseEntity.status(httpStatus).body(response);
    }
    
    /**
     * 에러 응답 생성 (400 Bad Request)
     * @param message 에러 메시지
     * @return ResponseEntity<Map<String, Object>>
     */
    public static ResponseEntity<Map<String, Object>> badRequest(String message) {
        return error("400", message, HttpStatus.BAD_REQUEST);
    }
    
    /**
     * 에러 응답 생성 (401 Unauthorized)
     * @param message 에러 메시지
     * @return ResponseEntity<Map<String, Object>>
     */
    public static ResponseEntity<Map<String, Object>> unauthorized(String message) {
        return error("401", message, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * 에러 응답 생성 (403 Forbidden)
     * @param message 에러 메시지
     * @return ResponseEntity<Map<String, Object>>
     */
    public static ResponseEntity<Map<String, Object>> forbidden(String message) {
        return error("403", message, HttpStatus.FORBIDDEN);
    }
    
    /**
     * 에러 응답 생성 (404 Not Found)
     * @param message 에러 메시지
     * @return ResponseEntity<Map<String, Object>>
     */
    public static ResponseEntity<Map<String, Object>> notFound(String message) {
        return error("404", message, HttpStatus.NOT_FOUND);
    }
    
    /**
     * 에러 응답 생성 (500 Internal Server Error)
     * @param message 에러 메시지
     * @return ResponseEntity<Map<String, Object>>
     */
    public static ResponseEntity<Map<String, Object>> internalServerError(String message) {
        return error("500", message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * 예외 발생 시 에러 응답 생성
     * @param logger Logger 인스턴스
     * @param e 예외 객체
     * @param operationName 작업 이름
     * @return ResponseEntity<Map<String, Object>>
     */
    public static ResponseEntity<Map<String, Object>> handleException(Logger logger, Exception e, String operationName) {
        StringUtil.logError(logger, operationName + " 중 오류 발생", e);
        return internalServerError(operationName + " 처리 중 오류가 발생했습니다.");
    }
    
    /**
     * 세션 만료 응답 생성
     * @return ResponseEntity<Map<String, Object>>
     */
    public static ResponseEntity<Map<String, Object>> sessionExpired() {
        return unauthorized("세션이 만료되었습니다. 다시 로그인해주세요.");
    }
    
    /**
     * 권한 없음 응답 생성
     * @return ResponseEntity<Map<String, Object>>
     */
    public static ResponseEntity<Map<String, Object>> accessDenied() {
        return forbidden("접근 권한이 없습니다.");
    }
    
    /**
     * 유효성 검사 실패 응답 생성
     * @param message 유효성 검사 실패 메시지
     * @return ResponseEntity<Map<String, Object>>
     */
    public static ResponseEntity<Map<String, Object>> validationError(String message) {
        return badRequest("입력 데이터가 유효하지 않습니다: " + message);
    }
}
