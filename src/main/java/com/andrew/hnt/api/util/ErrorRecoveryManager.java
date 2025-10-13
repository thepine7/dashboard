package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Lazy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 통합 에러 복구 관리자
 * 모든 에러 타입에 대한 통합된 복구 전략을 제공
 */
@Component
public class ErrorRecoveryManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorRecoveryManager.class);
    
    @Autowired
    @Lazy
    private UnifiedErrorHandler errorHandler;
    
    // 에러 복구 전략 맵
    private final Map<ErrorRecoveryStrategy.ErrorType, List<ErrorRecoveryStrategy>> recoveryStrategies = new ConcurrentHashMap<>();
    
    // 복구 시도 기록
    private final Map<String, RecoveryAttempt> recoveryAttempts = new ConcurrentHashMap<>();
    
    // 통계 정보
    private final AtomicLong totalRecoveryAttempts = new AtomicLong(0);
    private final AtomicLong successfulRecoveries = new AtomicLong(0);
    private final AtomicLong failedRecoveries = new AtomicLong(0);
    
    // 최대 재시도 횟수
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RECOVERY_TIMEOUT = 30000; // 30초
    
    /**
     * 에러 복구 전략 등록
     * @param strategy 복구 전략
     */
    public void registerRecoveryStrategy(ErrorRecoveryStrategy strategy) {
        ErrorRecoveryStrategy.ErrorType errorType = strategy.getErrorType();
        recoveryStrategies.computeIfAbsent(errorType, k -> new ArrayList<>()).add(strategy);
        
        // 우선순위별 정렬
        recoveryStrategies.get(errorType).sort(Comparator.comparingInt(ErrorRecoveryStrategy::getPriority));
        
        logger.info("에러 복구 전략 등록 완료 - 타입: {}, 우선순위: {}", 
                   errorType.getDescription(), strategy.getPriority());
    }
    
    /**
     * 에러 복구 시도
     * @param errorContext 에러 컨텍스트
     * @return 복구 결과
     */
    public ErrorRecoveryStrategy.RecoveryResult attemptRecovery(ErrorRecoveryStrategy.ErrorContext errorContext) {
        String errorId = generateErrorId(errorContext);
        RecoveryAttempt attempt = getOrCreateRecoveryAttempt(errorId, errorContext);
        
        // 최대 재시도 횟수 확인
        if (attempt.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
            logger.warn("최대 재시도 횟수 초과 - 에러 ID: {}, 재시도 횟수: {}", errorId, attempt.getRetryCount());
            return new ErrorRecoveryStrategy.RecoveryResult(false, "최대 재시도 횟수를 초과했습니다.");
        }
        
        // 복구 시도 기록
        attempt.incrementRetryCount();
        totalRecoveryAttempts.incrementAndGet();
        
        logger.info("에러 복구 시도 시작 - 에러 ID: {}, 타입: {}, 재시도 횟수: {}", 
                   errorId, errorContext.getErrorType(), attempt.getRetryCount());
        
        try {
            // 에러 타입에 따른 복구 전략 선택
            ErrorRecoveryStrategy.ErrorType errorType = ErrorRecoveryStrategy.ErrorType.valueOf(errorContext.getErrorType());
            List<ErrorRecoveryStrategy> strategies = recoveryStrategies.get(errorType);
            
            if (strategies == null || strategies.isEmpty()) {
                logger.warn("해당 에러 타입에 대한 복구 전략이 없음 - 타입: {}", errorType);
                return new ErrorRecoveryStrategy.RecoveryResult(false, "복구 전략이 없습니다.");
            }
            
            // 복구 전략 순차 실행
            for (ErrorRecoveryStrategy strategy : strategies) {
                if (!strategy.canRecover(errorContext)) {
                    logger.debug("복구 전략 건너뜀 - 전략: {}, 에러 ID: {}", 
                               strategy.getClass().getSimpleName(), errorId);
                    continue;
                }
                
                logger.info("복구 전략 실행 - 전략: {}, 에러 ID: {}", 
                           strategy.getClass().getSimpleName(), errorId);
                
                ErrorRecoveryStrategy.RecoveryResult result = strategy.attemptRecovery(errorContext);
                
                if (result.isSuccess()) {
                    successfulRecoveries.incrementAndGet();
                    attempt.setLastSuccessfulStrategy(strategy.getClass().getSimpleName());
                    attempt.setLastRecoveryTime(System.currentTimeMillis());
                    
                    logger.info("에러 복구 성공 - 전략: {}, 에러 ID: {}, 메시지: {}", 
                               strategy.getClass().getSimpleName(), errorId, result.getMessage());
                    
                    return result;
                } else {
                    logger.warn("복구 전략 실패 - 전략: {}, 에러 ID: {}, 메시지: {}", 
                               strategy.getClass().getSimpleName(), errorId, result.getMessage());
                }
            }
            
            // 모든 전략 실패
            failedRecoveries.incrementAndGet();
            logger.error("모든 복구 전략 실패 - 에러 ID: {}, 타입: {}", errorId, errorType);
            
            return new ErrorRecoveryStrategy.RecoveryResult(false, "모든 복구 전략이 실패했습니다.");
            
        } catch (Exception e) {
            failedRecoveries.incrementAndGet();
            errorHandler.logError("에러 복구 시도 중 예외 발생", e);
            
            return new ErrorRecoveryStrategy.RecoveryResult(false, 
                "복구 시도 중 예외가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 에러 복구 가능 여부 확인
     * @param errorContext 에러 컨텍스트
     * @return 복구 가능 여부
     */
    public boolean canRecover(ErrorRecoveryStrategy.ErrorContext errorContext) {
        try {
            ErrorRecoveryStrategy.ErrorType errorType = ErrorRecoveryStrategy.ErrorType.valueOf(errorContext.getErrorType());
            List<ErrorRecoveryStrategy> strategies = recoveryStrategies.get(errorType);
            
            if (strategies == null || strategies.isEmpty()) {
                return false;
            }
            
            return strategies.stream().anyMatch(strategy -> strategy.canRecover(errorContext));
            
        } catch (Exception e) {
            logger.warn("복구 가능 여부 확인 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 복구 시도 기록 조회
     * @param errorId 에러 ID
     * @return 복구 시도 기록
     */
    public RecoveryAttempt getRecoveryAttempt(String errorId) {
        return recoveryAttempts.get(errorId);
    }
    
    /**
     * 복구 통계 조회
     * @return 복구 통계
     */
    public Map<String, Object> getRecoveryStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRecoveryAttempts", totalRecoveryAttempts.get());
        stats.put("successfulRecoveries", successfulRecoveries.get());
        stats.put("failedRecoveries", failedRecoveries.get());
        stats.put("successRate", calculateSuccessRate());
        stats.put("activeRecoveryAttempts", recoveryAttempts.size());
        stats.put("registeredStrategies", getRegisteredStrategiesCount());
        return stats;
    }
    
    /**
     * 복구 시도 기록 초기화
     * @param errorId 에러 ID
     */
    public void clearRecoveryAttempt(String errorId) {
        recoveryAttempts.remove(errorId);
    }
    
    /**
     * 모든 복구 시도 기록 초기화
     */
    public void clearAllRecoveryAttempts() {
        recoveryAttempts.clear();
    }
    
    /**
     * 에러 ID 생성
     */
    private String generateErrorId(ErrorRecoveryStrategy.ErrorContext errorContext) {
        return String.format("%s_%d_%d", 
            errorContext.getErrorType(), 
            errorContext.getFirstErrorTime(),
            errorContext.hashCode());
    }
    
    /**
     * 복구 시도 기록 조회 또는 생성
     */
    private RecoveryAttempt getOrCreateRecoveryAttempt(String errorId, ErrorRecoveryStrategy.ErrorContext errorContext) {
        return recoveryAttempts.computeIfAbsent(errorId, k -> new RecoveryAttempt(errorContext));
    }
    
    /**
     * 성공률 계산
     */
    private double calculateSuccessRate() {
        long total = totalRecoveryAttempts.get();
        if (total == 0) return 0.0;
        return (double) successfulRecoveries.get() / total * 100;
    }
    
    /**
     * 등록된 전략 수 조회
     */
    private int getRegisteredStrategiesCount() {
        return recoveryStrategies.values().stream()
            .mapToInt(List::size)
            .sum();
    }
    
    /**
     * 복구 시도 기록 클래스
     */
    public static class RecoveryAttempt {
        private final String errorId;
        private final ErrorRecoveryStrategy.ErrorContext originalContext;
        private int retryCount;
        private String lastSuccessfulStrategy;
        private long lastRecoveryTime;
        private final long firstAttemptTime;
        
        public RecoveryAttempt(ErrorRecoveryStrategy.ErrorContext originalContext) {
            this.errorId = UUID.randomUUID().toString();
            this.originalContext = originalContext;
            this.retryCount = 0;
            this.firstAttemptTime = System.currentTimeMillis();
        }
        
        public void incrementRetryCount() {
            this.retryCount++;
        }
        
        // Getters and Setters
        public String getErrorId() { return errorId; }
        public ErrorRecoveryStrategy.ErrorContext getOriginalContext() { return originalContext; }
        public int getRetryCount() { return retryCount; }
        public String getLastSuccessfulStrategy() { return lastSuccessfulStrategy; }
        public long getLastRecoveryTime() { return lastRecoveryTime; }
        public long getFirstAttemptTime() { return firstAttemptTime; }
        
        public void setLastSuccessfulStrategy(String lastSuccessfulStrategy) {
            this.lastSuccessfulStrategy = lastSuccessfulStrategy;
        }
        
        public void setLastRecoveryTime(long lastRecoveryTime) {
            this.lastRecoveryTime = lastRecoveryTime;
        }
    }
}
