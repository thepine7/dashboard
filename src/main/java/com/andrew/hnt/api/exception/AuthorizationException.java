package com.andrew.hnt.api.exception;

/**
 * 권한 예외
 * 사용자 권한 부족 시 발생
 */
public class AuthorizationException extends RuntimeException {
    
    private final String errorCode;
    
    public AuthorizationException(String message) {
        super(message);
        this.errorCode = "AUTHZ_ERROR";
    }
    
    public AuthorizationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
