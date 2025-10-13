package com.andrew.hnt.api.exception;

import java.util.List;
import java.util.Map;

/**
 * 유효성 검사 예외 클래스
 * 입력 데이터 유효성 검사 관련 오류를 처리
 */
public class ValidationException extends BaseException {
    
    private static final long serialVersionUID = 1L;
    
    private List<String> validationErrors;
    private Map<String, String> fieldErrors;
    
    public ValidationException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
    
    public ValidationException(String errorCode, String errorMessage, Throwable cause) {
        super(errorCode, errorMessage, cause);
    }
    
    public ValidationException(String errorCode, String errorMessage, List<String> validationErrors) {
        super(errorCode, errorMessage);
        this.validationErrors = validationErrors;
    }
    
    public ValidationException(String errorCode, String errorMessage, Map<String, String> fieldErrors) {
        super(errorCode, errorMessage);
        this.fieldErrors = fieldErrors;
    }
    
    public List<String> getValidationErrors() {
        return validationErrors;
    }
    
    public void setValidationErrors(List<String> validationErrors) {
        this.validationErrors = validationErrors;
    }
    
    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
    
    public void setFieldErrors(Map<String, String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
    
    // 자주 사용되는 유효성 검사 예외 생성 메서드들
    public static ValidationException requiredField(String fieldName) {
        return new ValidationException("VALIDATION_REQUIRED_FIELD", "필수 필드가 누락되었습니다: " + fieldName);
    }
    
    public static ValidationException invalidFormat(String fieldName) {
        return new ValidationException("VALIDATION_INVALID_FORMAT", "유효하지 않은 형식입니다: " + fieldName);
    }
    
    public static ValidationException invalidLength(String fieldName, int minLength, int maxLength) {
        return new ValidationException("VALIDATION_INVALID_LENGTH", 
            "길이가 유효하지 않습니다: " + fieldName + " (최소: " + minLength + ", 최대: " + maxLength + ")");
    }
    
    public static ValidationException invalidRange(String fieldName, Object minValue, Object maxValue) {
        return new ValidationException("VALIDATION_INVALID_RANGE", 
            "범위가 유효하지 않습니다: " + fieldName + " (최소: " + minValue + ", 최대: " + maxValue + ")");
    }
    
    public static ValidationException invalidValue(String fieldName, String value) {
        return new ValidationException("VALIDATION_INVALID_VALUE", 
            "유효하지 않은 값입니다: " + fieldName + " = " + value);
    }
    
    public static ValidationException duplicateValue(String fieldName, String value) {
        return new ValidationException("VALIDATION_DUPLICATE_VALUE", 
            "중복된 값입니다: " + fieldName + " = " + value);
    }
    
    public static ValidationException multipleErrors(List<String> errors) {
        return new ValidationException("VALIDATION_MULTIPLE_ERRORS", 
            "여러 유효성 검사 오류가 발생했습니다.", errors);
    }
    
    public static ValidationException fieldErrors(Map<String, String> fieldErrors) {
        return new ValidationException("VALIDATION_FIELD_ERRORS", 
            "필드 유효성 검사 오류가 발생했습니다.", fieldErrors);
    }
}