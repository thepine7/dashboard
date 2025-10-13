package com.andrew.hnt.api.exception;

/**
 * 데이터베이스 예외 클래스
 * 데이터베이스 관련 오류를 처리
 */
public class DatabaseException extends BaseException {
    
    private static final long serialVersionUID = 1L;
    
    public DatabaseException(String errorCode, String errorMessage) {
        super(errorCode, errorMessage);
    }
    
    public DatabaseException(String errorCode, String errorMessage, Throwable cause) {
        super(errorCode, errorMessage, cause);
    }
    
    // 자주 사용되는 데이터베이스 예외 생성 메서드들
    public static DatabaseException connectionFailed() {
        return new DatabaseException("DB_CONNECTION_FAILED", "데이터베이스 연결에 실패했습니다.");
    }
    
    public static DatabaseException connectionFailed(Throwable cause) {
        return new DatabaseException("DB_CONNECTION_FAILED", "데이터베이스 연결에 실패했습니다.", cause);
    }
    
    public static DatabaseException queryFailed(String query) {
        return new DatabaseException("DB_QUERY_FAILED", "쿼리 실행에 실패했습니다: " + query);
    }
    
    public static DatabaseException queryFailed(String query, Throwable cause) {
        return new DatabaseException("DB_QUERY_FAILED", "쿼리 실행에 실패했습니다: " + query, cause);
    }
    
    public static DatabaseException transactionFailed() {
        return new DatabaseException("DB_TRANSACTION_FAILED", "트랜잭션 처리에 실패했습니다.");
    }
    
    public static DatabaseException transactionFailed(Throwable cause) {
        return new DatabaseException("DB_TRANSACTION_FAILED", "트랜잭션 처리에 실패했습니다.", cause);
    }
    
    public static DatabaseException constraintViolation(String constraint) {
        return new DatabaseException("DB_CONSTRAINT_VIOLATION", "제약 조건 위반: " + constraint);
    }
    
    public static DatabaseException constraintViolation(String constraint, Throwable cause) {
        return new DatabaseException("DB_CONSTRAINT_VIOLATION", "제약 조건 위반: " + constraint, cause);
    }
    
    public static DatabaseException timeout() {
        return new DatabaseException("DB_TIMEOUT", "데이터베이스 작업 시간 초과");
    }
    
    public static DatabaseException timeout(Throwable cause) {
        return new DatabaseException("DB_TIMEOUT", "데이터베이스 작업 시간 초과", cause);
    }
    
    public static DatabaseException deadlock() {
        return new DatabaseException("DB_DEADLOCK", "데이터베이스 데드락 발생");
    }
    
    public static DatabaseException deadlock(Throwable cause) {
        return new DatabaseException("DB_DEADLOCK", "데이터베이스 데드락 발생", cause);
    }
}