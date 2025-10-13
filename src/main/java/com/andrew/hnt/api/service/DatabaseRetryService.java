package com.andrew.hnt.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * 데이터베이스 연결 재시도 및 폴백 처리 서비스
 */
@Service
public class DatabaseRetryService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseRetryService.class);
    
    @Autowired
    private DataSource dataSource;
    
    
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1초
    private static final long MAX_RETRY_DELAY_MS = 5000; // 5초
    
    /**
     * 데이터베이스 연결 상태 확인
     */
    public boolean isDatabaseConnected() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5); // 5초 타임아웃
        } catch (SQLException e) {
            logger.error("데이터베이스 연결 확인 실패", e);
            return false;
        }
    }
    
    /**
     * 재시도 로직이 포함된 데이터베이스 작업 실행
     */
    public <T> T executeWithRetry(DatabaseOperation<T> operation) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                logger.debug("데이터베이스 작업 시도 {}/{}", attempt, MAX_RETRY_ATTEMPTS);
                return operation.execute();
            } catch (DataAccessException e) {
                lastException = e;
                logger.warn("데이터베이스 작업 실패 (시도 {}/{}): {}", attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    long delay = calculateRetryDelay(attempt);
                    logger.info("{}ms 후 재시도...", delay);
                    try {
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("재시도 중 인터럽트 발생", ie);
                    }
                }
            }
        }
        
        logger.error("데이터베이스 작업 최대 재시도 횟수 초과", lastException);
        throw new RuntimeException("데이터베이스 작업 실패 - 최대 재시도 횟수 초과", lastException);
    }
    
    /**
     * 지수 백오프 방식으로 재시도 지연 시간 계산
     */
    private long calculateRetryDelay(int attempt) {
        long delay = RETRY_DELAY_MS * (long) Math.pow(2, attempt - 1);
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }
    
    /**
     * 데이터베이스 연결 풀 상태 확인
     */
    public String getConnectionPoolStatus() {
        try {
            if (dataSource instanceof com.zaxxer.hikari.HikariDataSource) {
                com.zaxxer.hikari.HikariDataSource hikariDataSource = (com.zaxxer.hikari.HikariDataSource) dataSource;
                return String.format("Active: %d, Idle: %d, Total: %d, Waiting: %d",
                    hikariDataSource.getHikariPoolMXBean().getActiveConnections(),
                    hikariDataSource.getHikariPoolMXBean().getIdleConnections(),
                    hikariDataSource.getHikariPoolMXBean().getTotalConnections(),
                    hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
            }
        } catch (Exception e) {
            logger.warn("연결 풀 상태 확인 실패", e);
        }
        return "연결 풀 상태 확인 불가";
    }
    
    /**
     * 데이터베이스 작업 인터페이스
     */
    @FunctionalInterface
    public interface DatabaseOperation<T> {
        T execute() throws Exception;
    }
}
