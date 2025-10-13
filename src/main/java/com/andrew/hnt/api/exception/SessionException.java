package com.andrew.hnt.api.exception;

/**
 * 세션 예외 클래스
 * 세션 관련 오류를 처리
 */
public class SessionException extends BaseException {
    
    private static final long serialVersionUID = 1L;
    
    public SessionException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
    
    public SessionException(String errorCode, String errorMessage, Throwable cause) {
        super(errorCode, errorMessage, cause);
    }
    
    // 자주 사용되는 세션 예외 생성 메서드들
    public static SessionException sessionExpired() {
        return new SessionException("SESSION_EXPIRED", "세션이 만료되었습니다.");
    }
    
    public static SessionException sessionNotFound() {
        return new SessionException("SESSION_NOT_FOUND", "세션을 찾을 수 없습니다.");
    }
    
    public static SessionException sessionInvalid() {
        return new SessionException("SESSION_INVALID", "유효하지 않은 세션입니다.");
    }
    
    public static SessionException sessionTimeout() {
        return new SessionException("SESSION_TIMEOUT", "세션 시간이 초과되었습니다.");
    }
    
    public static SessionException sessionCreationFailed() {
        return new SessionException("SESSION_CREATION_FAILED", "세션 생성에 실패했습니다.");
    }
    
    public static SessionException sessionCreationFailed(Throwable cause) {
        return new SessionException("SESSION_CREATION_FAILED", "세션 생성에 실패했습니다.", cause);
    }
}
