package com.andrew.hnt.api.exception;

/**
 * 기본 예외 클래스
 * 모든 커스텀 예외의 부모 클래스
 * RuntimeException을 상속하여 unchecked exception으로 처리
 */
public abstract class BaseException extends RuntimeException {
    
    private static final long serialVersionUID = 1L;
    
    private String errorCode;
    private String errorMessage;
    private Throwable cause;
    
    public BaseException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
    
    public BaseException(String errorCode, String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.cause = cause;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Throwable getCause() {
        return cause;
    }
    
    public void setCause(Throwable cause) {
        this.cause = cause;
    }
    
    @Override
    public String getMessage() {
        return errorMessage;
    }
}
