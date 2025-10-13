package com.andrew.hnt.api.exception;

/**
 * 비즈니스 예외 클래스
 * 비즈니스 로직 관련 오류를 처리
 */
public class BusinessException extends BaseException {
    
    private static final long serialVersionUID = 1L;
    
    public BusinessException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
    
    public BusinessException(String errorCode, String errorMessage, Throwable cause) {
        super(errorCode, errorMessage, cause);
    }
    
    // 자주 사용되는 비즈니스 예외 생성 메서드들
    public static BusinessException userNotFound(String userId) {
        return new BusinessException("USER_NOT_FOUND", "사용자를 찾을 수 없습니다: " + userId);
    }
    
    public static BusinessException sensorNotFound(String sensorId) {
        return new BusinessException("SENSOR_NOT_FOUND", "센서를 찾을 수 없습니다: " + sensorId);
    }
    
    public static BusinessException invalidParameter(String parameterName) {
        return new BusinessException("INVALID_PARAMETER", "유효하지 않은 파라미터입니다: " + parameterName);
    }
    
    public static BusinessException duplicateData(String dataType) {
        return new BusinessException("DUPLICATE_DATA", "중복된 데이터입니다: " + dataType);
    }
    
    public static BusinessException dataNotFound(String dataType) {
        return new BusinessException("DATA_NOT_FOUND", "데이터를 찾을 수 없습니다: " + dataType);
    }
    
    public static BusinessException operationFailed(String operation) {
        return new BusinessException("OPERATION_FAILED", "작업이 실패했습니다: " + operation);
    }
    
    public static BusinessException invalidState(String currentState) {
        return new BusinessException("INVALID_STATE", "유효하지 않은 상태입니다: " + currentState);
    }
}