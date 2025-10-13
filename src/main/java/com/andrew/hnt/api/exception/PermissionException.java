package com.andrew.hnt.api.exception;

/**
 * 권한 예외 클래스
 * 권한 관련 오류를 처리
 */
public class PermissionException extends BaseException {
    
    private static final long serialVersionUID = 1L;
    
    public PermissionException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
    
    public PermissionException(String errorCode, String errorMessage, Throwable cause) {
        super(errorCode, errorMessage, cause);
    }
    
    // 자주 사용되는 권한 예외 생성 메서드들
    public static PermissionException accessDenied() {
        return new PermissionException("ACCESS_DENIED", "접근이 거부되었습니다.");
    }
    
    public static PermissionException accessDenied(String resource) {
        return new PermissionException("ACCESS_DENIED", "접근이 거부되었습니다: " + resource);
    }
    
    public static PermissionException insufficientPermissions() {
        return new PermissionException("INSUFFICIENT_PERMISSIONS", "권한이 부족합니다.");
    }
    
    public static PermissionException insufficientPermissions(String requiredPermission) {
        return new PermissionException("INSUFFICIENT_PERMISSIONS", "권한이 부족합니다: " + requiredPermission);
    }
    
    public static PermissionException userNotAuthorized() {
        return new PermissionException("USER_NOT_AUTHORIZED", "사용자가 인증되지 않았습니다.");
    }
    
    public static PermissionException userNotAuthorized(String userId) {
        return new PermissionException("USER_NOT_AUTHORIZED", "사용자가 인증되지 않았습니다: " + userId);
    }
    
    public static PermissionException operationNotAllowed() {
        return new PermissionException("OPERATION_NOT_ALLOWED", "허용되지 않은 작업입니다.");
    }
    
    public static PermissionException operationNotAllowed(String operation) {
        return new PermissionException("OPERATION_NOT_ALLOWED", "허용되지 않은 작업입니다: " + operation);
    }
    
    public static PermissionException subAccountRestriction() {
        return new PermissionException("SUB_ACCOUNT_RESTRICTION", "부계정은 이 작업을 수행할 수 없습니다.");
    }
    
    public static PermissionException adminOnly() {
        return new PermissionException("ADMIN_ONLY", "관리자만 접근할 수 있습니다.");
    }
}
