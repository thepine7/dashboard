package com.andrew.hnt.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 데이터베이스 폴백 처리 서비스
 * 데이터베이스 연결 실패 시 임시 데이터 저장 및 복구 처리
 */
@Service
public class DatabaseFallbackService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseFallbackService.class);
    
    @Autowired
    private DatabaseRetryService databaseRetryService;
    
    // 메모리 기반 임시 저장소
    private final Map<String, Object> fallbackCache = new ConcurrentHashMap<>();
    private final AtomicLong fallbackCount = new AtomicLong(0);
    private final AtomicLong recoveryCount = new AtomicLong(0);
    
    /**
     * 폴백 모드 활성화 여부 확인
     */
    public boolean isFallbackMode() {
        return !databaseRetryService.isDatabaseConnected();
    }
    
    /**
     * 폴백 데이터 저장
     */
    public void storeFallbackData(String key, Object data) {
        try {
            fallbackCache.put(key, data);
            fallbackCount.incrementAndGet();
            logger.info("폴백 데이터 저장: key={}, 총 폴백 데이터 수={}", key, fallbackCount.get());
        } catch (Exception e) {
            logger.error("폴백 데이터 저장 실패: key={}", key, e);
        }
    }
    
    /**
     * 폴백 데이터 조회
     */
    public Object getFallbackData(String key) {
        return fallbackCache.get(key);
    }
    
    /**
     * 폴백 데이터 제거
     */
    public void removeFallbackData(String key) {
        fallbackCache.remove(key);
        logger.debug("폴백 데이터 제거: key={}", key);
    }
    
    /**
     * 폴백 데이터 복구 시도
     */
    public void attemptRecovery() {
        if (fallbackCache.isEmpty()) {
            logger.debug("복구할 폴백 데이터가 없습니다.");
            return;
        }
        
        logger.info("폴백 데이터 복구 시도 시작: {}개 데이터", fallbackCache.size());
        
        int successCount = 0;
        int failureCount = 0;
        
        for (Map.Entry<String, Object> entry : fallbackCache.entrySet()) {
            try {
                // 실제 데이터베이스에 저장 시도
                recoverData(entry.getKey(), entry.getValue());
                successCount++;
                fallbackCache.remove(entry.getKey());
            } catch (Exception e) {
                failureCount++;
                logger.warn("폴백 데이터 복구 실패: key={}, error={}", entry.getKey(), e.getMessage());
            }
        }
        
        recoveryCount.addAndGet(successCount);
        logger.info("폴백 데이터 복구 완료: 성공={}, 실패={}, 총 복구={}", 
            successCount, failureCount, recoveryCount.get());
    }
    
    /**
     * 실제 데이터 복구 로직 (구체적인 구현은 필요에 따라 확장)
     */
    private void recoverData(String key, Object data) throws Exception {
        // 여기서는 간단한 로깅만 수행
        // 실제 구현에서는 데이터베이스에 저장하는 로직을 추가
        logger.debug("데이터 복구: key={}, data={}", key, data);
        
        // 예시: 센서 데이터 복구
        if (key.startsWith("sensor_data_")) {
            // 센서 데이터 복구 로직
            logger.info("센서 데이터 복구: {}", key);
        }
        // 예시: 사용자 데이터 복구
        else if (key.startsWith("user_data_")) {
            // 사용자 데이터 복구 로직
            logger.info("사용자 데이터 복구: {}", key);
        }
    }
    
    /**
     * 폴백 통계 정보 조회
     */
    public Map<String, Object> getFallbackStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("fallbackMode", isFallbackMode());
        stats.put("fallbackDataCount", fallbackCache.size());
        stats.put("totalFallbackCount", fallbackCount.get());
        stats.put("totalRecoveryCount", recoveryCount.get());
        stats.put("connectionPoolStatus", databaseRetryService.getConnectionPoolStatus());
        return stats;
    }
    
    /**
     * 폴백 캐시 초기화
     */
    public void clearFallbackCache() {
        int size = fallbackCache.size();
        fallbackCache.clear();
        logger.info("폴백 캐시 초기화: {}개 데이터 제거", size);
    }
}
