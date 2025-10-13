package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 통합 권한 검증 관리자
 * 모든 권한 타입에 대한 통합된 검증 전략을 제공
 */
@Component
public class PermissionValidationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PermissionValidationManager.class);
    
    @Autowired
    private UnifiedErrorHandler errorHandler;
    
    // 권한 검증 전략 맵
    private final Map<PermissionValidationStrategy.PermissionType, List<PermissionValidationStrategy>> validationStrategies = new ConcurrentHashMap<>();
    
    // 검증 시도 기록
    private final Map<String, ValidationAttempt> validationAttempts = new ConcurrentHashMap<>();
    
    // 통계 정보
    private final AtomicLong totalValidationAttempts = new AtomicLong(0);
    private final AtomicLong successfulValidations = new AtomicLong(0);
    private final AtomicLong failedValidations = new AtomicLong(0);
    
    // 최대 재시도 횟수
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long VALIDATION_TIMEOUT = 10000; // 10초
    
    /**
     * 권한 검증 전략 등록
     * @param strategy 검증 전략
     */
    public void registerValidationStrategy(PermissionValidationStrategy strategy) {
        PermissionValidationStrategy.PermissionType permissionType = strategy.getPermissionType();
        validationStrategies.computeIfAbsent(permissionType, k -> new ArrayList<>()).add(strategy);
        
        // 우선순위별 정렬
        validationStrategies.get(permissionType).sort(Comparator.comparingInt(PermissionValidationStrategy::getPriority));
        
        logger.info("권한 검증 전략 등록 완료 - 타입: {}, 우선순위: {}", 
                   permissionType.getDescription(), strategy.getPriority());
    }
    
    /**
     * 권한 검증 시도
     * @param context 권한 검증 컨텍스트
     * @return 검증 결과
     */
    public PermissionValidationStrategy.ValidationResult validatePermission(PermissionValidationStrategy.PermissionContext context) {
        String validationId = generateValidationId(context);
        ValidationAttempt attempt = getOrCreateValidationAttempt(validationId, context);
        
        // 최대 재시도 횟수 확인
        if (attempt.getRetryCount() >= MAX_RETRY_ATTEMPTS) {
            logger.warn("최대 재시도 횟수 초과 - 검증 ID: {}, 재시도 횟수: {}", validationId, attempt.getRetryCount());
            return new PermissionValidationStrategy.ValidationResult(false, "최대 재시도 횟수를 초과했습니다.");
        }
        
        // 검증 시도 기록
        attempt.incrementRetryCount();
        totalValidationAttempts.incrementAndGet();
        
        logger.info("권한 검증 시도 시작 - 검증 ID: {}, 권한: {}, 재시도 횟수: {}", 
                   validationId, context.getRequiredPermission(), attempt.getRetryCount());
        
        try {
            // 권한 타입에 따른 검증 전략 선택
            PermissionValidationStrategy.PermissionType permissionType = determinePermissionType(context.getRequiredPermission());
            List<PermissionValidationStrategy> strategies = validationStrategies.get(permissionType);
            
            if (strategies == null || strategies.isEmpty()) {
                logger.warn("해당 권한 타입에 대한 검증 전략이 없음 - 타입: {}", permissionType);
                return new PermissionValidationStrategy.ValidationResult(false, "검증 전략이 없습니다.");
            }
            
            // 검증 전략 순차 실행
            for (PermissionValidationStrategy strategy : strategies) {
                if (!strategy.canValidate(context)) {
                    logger.debug("검증 전략 건너뜀 - 전략: {}, 검증 ID: {}", 
                               strategy.getClass().getSimpleName(), validationId);
                    continue;
                }
                
                logger.info("검증 전략 실행 - 전략: {}, 검증 ID: {}", 
                           strategy.getClass().getSimpleName(), validationId);
                
                PermissionValidationStrategy.ValidationResult result = strategy.validatePermission(context);
                
                if (result.isValid()) {
                    successfulValidations.incrementAndGet();
                    attempt.setLastSuccessfulStrategy(strategy.getClass().getSimpleName());
                    attempt.setLastValidationTime(System.currentTimeMillis());
                    
                    logger.info("권한 검증 성공 - 전략: {}, 검증 ID: {}, 메시지: {}", 
                               strategy.getClass().getSimpleName(), validationId, result.getMessage());
                    
                    return result;
                } else {
                    logger.warn("검증 전략 실패 - 전략: {}, 검증 ID: {}, 메시지: {}", 
                               strategy.getClass().getSimpleName(), validationId, result.getMessage());
                }
            }
            
            // 모든 전략 실패
            failedValidations.incrementAndGet();
            logger.error("모든 검증 전략 실패 - 검증 ID: {}, 권한: {}", validationId, context.getRequiredPermission());
            
            return new PermissionValidationStrategy.ValidationResult(false, "모든 검증 전략이 실패했습니다.");
            
        } catch (Exception e) {
            failedValidations.incrementAndGet();
            errorHandler.logError("권한 검증 시도 중 예외 발생", e);
            
            return new PermissionValidationStrategy.ValidationResult(false, 
                "검증 시도 중 예외가 발생했습니다: " + e.getMessage());
        }
    }
    
    /**
     * 권한 검증 가능 여부 확인
     * @param context 권한 검증 컨텍스트
     * @return 검증 가능 여부
     */
    public boolean canValidate(PermissionValidationStrategy.PermissionContext context) {
        try {
            PermissionValidationStrategy.PermissionType permissionType = determinePermissionType(context.getRequiredPermission());
            List<PermissionValidationStrategy> strategies = validationStrategies.get(permissionType);
            
            if (strategies == null || strategies.isEmpty()) {
                return false;
            }
            
            return strategies.stream().anyMatch(strategy -> strategy.canValidate(context));
            
        } catch (Exception e) {
            logger.warn("검증 가능 여부 확인 중 오류 발생", e);
            return false;
        }
    }
    
    /**
     * 검증 시도 기록 조회
     * @param validationId 검증 ID
     * @return 검증 시도 기록
     */
    public ValidationAttempt getValidationAttempt(String validationId) {
        return validationAttempts.get(validationId);
    }
    
    /**
     * 검증 통계 조회
     * @return 검증 통계
     */
    public Map<String, Object> getValidationStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalValidationAttempts", totalValidationAttempts.get());
        stats.put("successfulValidations", successfulValidations.get());
        stats.put("failedValidations", failedValidations.get());
        stats.put("successRate", calculateSuccessRate());
        stats.put("activeValidationAttempts", validationAttempts.size());
        stats.put("registeredStrategies", getRegisteredStrategiesCount());
        return stats;
    }
    
    /**
     * 검증 시도 기록 초기화
     * @param validationId 검증 ID
     */
    public void clearValidationAttempt(String validationId) {
        validationAttempts.remove(validationId);
    }
    
    /**
     * 모든 검증 시도 기록 초기화
     */
    public void clearAllValidationAttempts() {
        validationAttempts.clear();
    }
    
    /**
     * 권한 타입 결정
     * @param requiredPermission 필요한 권한
     * @return 권한 타입
     */
    private PermissionValidationStrategy.PermissionType determinePermissionType(String requiredPermission) {
        if (requiredPermission == null || requiredPermission.isEmpty()) {
            return PermissionValidationStrategy.PermissionType.PAGE_ACCESS;
        }
        
        switch (requiredPermission.toUpperCase()) {
            case "A":
                return PermissionValidationStrategy.PermissionType.ADMIN_ONLY;
            case "U":
                return PermissionValidationStrategy.PermissionType.DATA_WRITE;
            case "B":
                return PermissionValidationStrategy.PermissionType.SUB_ACCOUNT_ACCESS;
            case "USER_MANAGEMENT":
                return PermissionValidationStrategy.PermissionType.USER_MANAGEMENT;
            case "DEVICE_MANAGEMENT":
                return PermissionValidationStrategy.PermissionType.DEVICE_MANAGEMENT;
            case "SENSOR_CONTROL":
                return PermissionValidationStrategy.PermissionType.SENSOR_CONTROL;
            default:
                return PermissionValidationStrategy.PermissionType.PAGE_ACCESS;
        }
    }
    
    /**
     * 검증 ID 생성
     */
    private String generateValidationId(PermissionValidationStrategy.PermissionContext context) {
        return String.format("%s_%d_%d", 
            context.getRequiredPermission(), 
            context.getFirstValidationTime(),
            context.hashCode());
    }
    
    /**
     * 검증 시도 기록 조회 또는 생성
     */
    private ValidationAttempt getOrCreateValidationAttempt(String validationId, PermissionValidationStrategy.PermissionContext context) {
        return validationAttempts.computeIfAbsent(validationId, k -> new ValidationAttempt(context));
    }
    
    /**
     * 성공률 계산
     */
    private double calculateSuccessRate() {
        long total = totalValidationAttempts.get();
        if (total == 0) return 0.0;
        return (double) successfulValidations.get() / total * 100;
    }
    
    /**
     * 등록된 전략 수 조회
     */
    private int getRegisteredStrategiesCount() {
        return validationStrategies.values().stream()
            .mapToInt(List::size)
            .sum();
    }
    
    /**
     * 검증 시도 기록 클래스
     */
    public static class ValidationAttempt {
        private final String validationId;
        private final PermissionValidationStrategy.PermissionContext originalContext;
        private int retryCount;
        private String lastSuccessfulStrategy;
        private long lastValidationTime;
        private final long firstAttemptTime;
        
        public ValidationAttempt(PermissionValidationStrategy.PermissionContext originalContext) {
            this.validationId = UUID.randomUUID().toString();
            this.originalContext = originalContext;
            this.retryCount = 0;
            this.firstAttemptTime = System.currentTimeMillis();
        }
        
        public void incrementRetryCount() {
            this.retryCount++;
        }
        
        // Getters and Setters
        public String getValidationId() { return validationId; }
        public PermissionValidationStrategy.PermissionContext getOriginalContext() { return originalContext; }
        public int getRetryCount() { return retryCount; }
        public String getLastSuccessfulStrategy() { return lastSuccessfulStrategy; }
        public long getLastValidationTime() { return lastValidationTime; }
        public long getFirstAttemptTime() { return firstAttemptTime; }
        
        public void setLastSuccessfulStrategy(String lastSuccessfulStrategy) {
            this.lastSuccessfulStrategy = lastSuccessfulStrategy;
        }
        
        public void setLastValidationTime(long lastValidationTime) {
            this.lastValidationTime = lastValidationTime;
        }
    }
}
