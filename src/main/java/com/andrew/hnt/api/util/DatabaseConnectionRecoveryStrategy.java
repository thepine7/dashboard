package com.andrew.hnt.api.util;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;

/**
 * 데이터베이스 연결 에러 복구 전략
 * 데이터베이스 연결 실패 시 다양한 복구 방법을 시도
 */
@Component
public class DatabaseConnectionRecoveryStrategy implements ErrorRecoveryStrategy {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionRecoveryStrategy.class);
    
    @Autowired
    private HikariDataSource hikariDataSource;
    
    @Override
    public RecoveryResult attemptRecovery(ErrorContext errorContext) {
        logger.info("데이터베이스 연결 복구 시도 시작 - 재시도 횟수: {}", errorContext.getRetryCount());
        
        try {
            // 1단계: 연결 풀 상태 확인
            if (hikariDataSource.isRunning()) {
                logger.info("데이터베이스 연결 풀이 정상 상태");
                
                // 테스트 연결 시도
                try (Connection connection = hikariDataSource.getConnection()) {
                    if (connection != null && !connection.isClosed()) {
                        logger.info("데이터베이스 연결 테스트 성공");
                        return new RecoveryResult(true, "데이터베이스 연결이 정상 상태입니다.");
                    }
                }
            }
            
            // 2단계: 연결 풀 재시작
            if (errorContext.getRetryCount() >= 1) {
                logger.info("데이터베이스 연결 풀 재시작 시도");
                
                try {
                    hikariDataSource.close();
                    Thread.sleep(2000); // 2초 대기
                    hikariDataSource.getConnection(); // 새 연결 시도
                    
                    logger.info("데이터베이스 연결 풀 재시작 성공");
                    return new RecoveryResult(true, "데이터베이스 연결 풀 재시작 성공");
                    
                } catch (Exception e) {
                    logger.warn("데이터베이스 연결 풀 재시작 실패", e);
                }
            }
            
            // 3단계: 연결 풀 설정 재검증
            if (errorContext.getRetryCount() >= 2) {
                logger.info("데이터베이스 연결 풀 설정 재검증 시도");
                
                try {
                    // 연결 풀 설정 확인
                    int maxPoolSize = hikariDataSource.getMaximumPoolSize();
                    int minIdle = hikariDataSource.getMinimumIdle();
                    long connectionTimeout = hikariDataSource.getConnectionTimeout();
                    
                    logger.info("연결 풀 설정 - 최대: {}, 최소: {}, 타임아웃: {}", 
                               maxPoolSize, minIdle, connectionTimeout);
                    
                    // 설정이 비정상적이면 기본값으로 재설정
                    if (maxPoolSize <= 0 || minIdle < 0 || connectionTimeout <= 0) {
                        logger.warn("연결 풀 설정이 비정상적임 - 기본값으로 재설정");
                        hikariDataSource.setMaximumPoolSize(20);
                        hikariDataSource.setMinimumIdle(5);
                        hikariDataSource.setConnectionTimeout(5000);
                    }
                    
                    // 재연결 시도
                    try (Connection connection = hikariDataSource.getConnection()) {
                        if (connection != null && !connection.isClosed()) {
                            logger.info("데이터베이스 연결 풀 설정 재검증 후 연결 성공");
                            return new RecoveryResult(true, "데이터베이스 연결 풀 설정 재검증 후 연결 성공");
                        }
                    }
                    
                } catch (Exception e) {
                    logger.warn("데이터베이스 연결 풀 설정 재검증 실패", e);
                }
            }
            
            logger.warn("데이터베이스 연결 복구 실패 - 모든 전략 시도 완료");
            return new RecoveryResult(false, "데이터베이스 연결 복구에 실패했습니다.");
            
        } catch (Exception e) {
            logger.error("데이터베이스 연결 복구 중 예외 발생", e);
            return new RecoveryResult(false, "데이터베이스 연결 복구 중 예외 발생: " + e.getMessage());
        }
    }
    
    @Override
    public boolean canRecover(ErrorContext errorContext) {
        // 데이터베이스 관련 에러만 복구 가능
        return "DATABASE_CONNECTION_ERROR".equals(errorContext.getErrorType()) ||
               "NETWORK_CONNECTION_ERROR".equals(errorContext.getErrorType());
    }
    
    @Override
    public int getPriority() {
        return 1; // 높은 우선순위
    }
    
    @Override
    public ErrorType getErrorType() {
        return ErrorType.DATABASE_CONNECTION_ERROR;
    }
}
