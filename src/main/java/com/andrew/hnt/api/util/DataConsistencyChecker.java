package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 데이터 불일치 감지 및 자동 복구 유틸리티
 * HnT Sensor API 프로젝트 전용
 * 
 * 프론트엔드-백엔드 간 데이터 불일치를 감지하고
 * 자동으로 복구하는 시스템
 */
@Component
public class DataConsistencyChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(DataConsistencyChecker.class);
    
    // 데이터 상태 추적
    private static final Map<String, DataState> dataStates = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastCheckTimes = new ConcurrentHashMap<>();
    
    // 불일치 감지 이력
    private static final Map<String, List<InconsistencyRecord>> inconsistencies = new ConcurrentHashMap<>();
    private static final int MAX_INCONSISTENCY_RECORDS = 100;
    
    // 자동 복구 통계
    private static final Map<String, AtomicLong> recoveryAttempts = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> recoverySuccesses = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> recoveryFailures = new ConcurrentHashMap<>();
    
    // 불일치 임계값 설정
    private static final long INCONSISTENCY_THRESHOLD_MS = 5000;  // 5초
    private static final int MAX_RECOVERY_ATTEMPTS = 3;           // 최대 복구 시도 횟수
    private static final long RECOVERY_COOLDOWN_MS = 10000;       // 10초 (복구 시도 간격)
    
    /**
     * 데이터 상태 클래스
     */
    public static class DataState {
        private final String key;
        private Object frontendValue;
        private Object backendValue;
        private long lastUpdateTime;
        private boolean isConsistent;
        private int recoveryAttemptCount;
        private long lastRecoveryTime;
        private Map<String, Object> metadata;
        
        public DataState(String key) {
            this.key = key;
            this.frontendValue = null;
            this.backendValue = null;
            this.lastUpdateTime = System.currentTimeMillis();
            this.isConsistent = true;
            this.recoveryAttemptCount = 0;
            this.lastRecoveryTime = 0;
            this.metadata = new HashMap<>();
        }
        
        // Getters and Setters
        public String getKey() { return key; }
        public Object getFrontendValue() { return frontendValue; }
        public void setFrontendValue(Object frontendValue) { 
            this.frontendValue = frontendValue; 
            this.lastUpdateTime = System.currentTimeMillis();
        }
        public Object getBackendValue() { return backendValue; }
        public void setBackendValue(Object backendValue) { 
            this.backendValue = backendValue;
            this.lastUpdateTime = System.currentTimeMillis();
        }
        public long getLastUpdateTime() { return lastUpdateTime; }
        public boolean isConsistent() { return isConsistent; }
        public void setConsistent(boolean consistent) { this.isConsistent = consistent; }
        public int getRecoveryAttemptCount() { return recoveryAttemptCount; }
        public void incrementRecoveryAttemptCount() { this.recoveryAttemptCount++; }
        public void resetRecoveryAttemptCount() { this.recoveryAttemptCount = 0; }
        public long getLastRecoveryTime() { return lastRecoveryTime; }
        public void setLastRecoveryTime(long lastRecoveryTime) { this.lastRecoveryTime = lastRecoveryTime; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void addMetadata(String key, Object value) { this.metadata.put(key, value); }
    }
    
    /**
     * 불일치 기록 클래스
     */
    public static class InconsistencyRecord {
        private final String key;
        private final long timestamp;
        private final Object frontendValue;
        private final Object backendValue;
        private final String inconsistencyType;
        private final String details;
        private boolean recovered;
        private long recoveryTime;
        
        public InconsistencyRecord(String key, Object frontendValue, Object backendValue, 
                                  String inconsistencyType, String details) {
            this.key = key;
            this.timestamp = System.currentTimeMillis();
            this.frontendValue = frontendValue;
            this.backendValue = backendValue;
            this.inconsistencyType = inconsistencyType;
            this.details = details;
            this.recovered = false;
            this.recoveryTime = 0;
        }
        
        // Getters and Setters
        public String getKey() { return key; }
        public long getTimestamp() { return timestamp; }
        public Object getFrontendValue() { return frontendValue; }
        public Object getBackendValue() { return backendValue; }
        public String getInconsistencyType() { return inconsistencyType; }
        public String getDetails() { return details; }
        public boolean isRecovered() { return recovered; }
        public void setRecovered(boolean recovered) { this.recovered = recovered; }
        public long getRecoveryTime() { return recoveryTime; }
        public void setRecoveryTime(long recoveryTime) { this.recoveryTime = recoveryTime; }
    }
    
    /**
     * 복구 결과 클래스
     */
    public static class RecoveryResult {
        private final boolean success;
        private final String message;
        private final Map<String, Object> details;
        
        public RecoveryResult(boolean success, String message) {
            this.success = success;
            this.message = message;
            this.details = new HashMap<>();
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Map<String, Object> getDetails() { return details; }
        public void addDetail(String key, Object value) { this.details.put(key, value); }
    }
    
    /**
     * 프론트엔드 데이터 업데이트
     * @param key 데이터 키
     * @param value 데이터 값
     */
    public static void updateFrontendData(String key, Object value) {
        DataState state = dataStates.computeIfAbsent(key, DataState::new);
        state.setFrontendValue(value);
        
        // 불일치 체크
        checkConsistency(key);
    }
    
    /**
     * 백엔드 데이터 업데이트
     * @param key 데이터 키
     * @param value 데이터 값
     */
    public static void updateBackendData(String key, Object value) {
        DataState state = dataStates.computeIfAbsent(key, DataState::new);
        state.setBackendValue(value);
        
        // 불일치 체크
        checkConsistency(key);
    }
    
    /**
     * 데이터 일관성 체크
     * @param key 데이터 키
     * @return 일관성 여부
     */
    public static boolean checkConsistency(String key) {
        DataState state = dataStates.get(key);
        if (state == null) {
            return true; // 데이터가 없으면 일관성 문제 없음
        }
        
        // 양쪽 데이터가 모두 있는 경우에만 체크
        if (state.getFrontendValue() == null || state.getBackendValue() == null) {
            return true;
        }
        
        // 값 비교
        boolean isConsistent = isValuesEqual(state.getFrontendValue(), state.getBackendValue());
        state.setConsistent(isConsistent);
        
        if (!isConsistent) {
            // 불일치 기록
            recordInconsistency(key, state.getFrontendValue(), state.getBackendValue(), 
                "VALUE_MISMATCH", "프론트엔드와 백엔드 값 불일치");
            
            logger.warn("데이터 불일치 감지: {} - Frontend: {}, Backend: {}", 
                key, state.getFrontendValue(), state.getBackendValue());
            
            // 자동 복구 시도
            attemptAutoRecovery(key);
        }
        
        lastCheckTimes.put(key, System.currentTimeMillis());
        return isConsistent;
    }
    
    /**
     * 값 동등성 비교
     * @param value1 값 1
     * @param value2 값 2
     * @return 동등 여부
     */
    private static boolean isValuesEqual(Object value1, Object value2) {
        if (value1 == null && value2 == null) {
            return true;
        }
        if (value1 == null || value2 == null) {
            return false;
        }
        
        // 숫자 비교 (타입이 다를 수 있음)
        if (value1 instanceof Number && value2 instanceof Number) {
            double d1 = ((Number) value1).doubleValue();
            double d2 = ((Number) value2).doubleValue();
            return Math.abs(d1 - d2) < 0.01; // 0.01 오차 허용
        }
        
        // 일반 비교
        return value1.equals(value2);
    }
    
    /**
     * 불일치 기록
     * @param key 데이터 키
     * @param frontendValue 프론트엔드 값
     * @param backendValue 백엔드 값
     * @param type 불일치 타입
     * @param details 상세 정보
     */
    private static void recordInconsistency(String key, Object frontendValue, Object backendValue, 
                                           String type, String details) {
        InconsistencyRecord record = new InconsistencyRecord(key, frontendValue, backendValue, type, details);
        
        inconsistencies.computeIfAbsent(key, k -> new ArrayList<>()).add(record);
        
        // 최대 기록 개수 유지
        List<InconsistencyRecord> records = inconsistencies.get(key);
        if (records.size() > MAX_INCONSISTENCY_RECORDS) {
            records.remove(0);
        }
    }
    
    /**
     * 자동 복구 시도
     * @param key 데이터 키
     * @return 복구 결과
     */
    public static RecoveryResult attemptAutoRecovery(String key) {
        DataState state = dataStates.get(key);
        if (state == null) {
            return new RecoveryResult(false, "데이터 상태를 찾을 수 없음");
        }
        
        // 복구 시도 제한 체크
        if (state.getRecoveryAttemptCount() >= MAX_RECOVERY_ATTEMPTS) {
            logger.warn("최대 복구 시도 횟수 초과: {}", key);
            return new RecoveryResult(false, "최대 복구 시도 횟수 초과");
        }
        
        // 복구 간격 체크
        long now = System.currentTimeMillis();
        if (now - state.getLastRecoveryTime() < RECOVERY_COOLDOWN_MS) {
            logger.debug("복구 시도 간격이 짧음: {}", key);
            return new RecoveryResult(false, "복구 시도 간격이 짧음");
        }
        
        // 복구 시도 카운트 증가
        state.incrementRecoveryAttemptCount();
        state.setLastRecoveryTime(now);
        recoveryAttempts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        
        try {
            // 백엔드 데이터를 신뢰할 수 있는 소스로 간주
            Object backendValue = state.getBackendValue();
            state.setFrontendValue(backendValue);
            state.setConsistent(true);
            state.resetRecoveryAttemptCount();
            
            // 복구 성공 기록
            recoverySuccesses.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
            
            // 불일치 기록 업데이트
            List<InconsistencyRecord> records = inconsistencies.get(key);
            if (records != null && !records.isEmpty()) {
                InconsistencyRecord lastRecord = records.get(records.size() - 1);
                if (!lastRecord.isRecovered()) {
                    lastRecord.setRecovered(true);
                    lastRecord.setRecoveryTime(now);
                }
            }
            
            logger.info("데이터 복구 성공: {} - 복구된 값: {}", key, backendValue);
            
            RecoveryResult result = new RecoveryResult(true, "데이터 복구 성공");
            result.addDetail("recoveredValue", backendValue);
            result.addDetail("recoveryTime", now);
            return result;
            
        } catch (Exception e) {
            logger.error("데이터 복구 실패: " + key, e);
            recoveryFailures.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
            
            RecoveryResult result = new RecoveryResult(false, "데이터 복구 실패: " + e.getMessage());
            result.addDetail("error", e.getMessage());
            return result;
        }
    }
    
    /**
     * 데이터 상태 조회
     * @param key 데이터 키
     * @return 데이터 상태
     */
    public static DataState getDataState(String key) {
        return dataStates.get(key);
    }
    
    /**
     * 모든 데이터 상태 조회
     * @return 모든 데이터 상태
     */
    public static Map<String, DataState> getAllDataStates() {
        return new HashMap<>(dataStates);
    }
    
    /**
     * 불일치 기록 조회
     * @param key 데이터 키
     * @param limit 조회 개수
     * @return 불일치 기록 목록
     */
    public static List<InconsistencyRecord> getInconsistencyRecords(String key, int limit) {
        List<InconsistencyRecord> records = inconsistencies.get(key);
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        
        int size = records.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(records.subList(fromIndex, size));
    }
    
    /**
     * 복구 통계 조회
     * @return 복구 통계
     */
    public static Map<String, Object> getRecoveryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 전체 통계
        long totalAttempts = recoveryAttempts.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();
        long totalSuccesses = recoverySuccesses.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();
        long totalFailures = recoveryFailures.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();
        
        stats.put("totalAttempts", totalAttempts);
        stats.put("totalSuccesses", totalSuccesses);
        stats.put("totalFailures", totalFailures);
        stats.put("successRate", totalAttempts > 0 ? 
            (double) totalSuccesses / totalAttempts * 100 : 0.0);
        
        // 키별 통계
        Map<String, Map<String, Long>> keyStats = new HashMap<>();
        recoveryAttempts.forEach((key, attempts) -> {
            Map<String, Long> keyStat = new HashMap<>();
            keyStat.put("attempts", attempts.get());
            keyStat.put("successes", recoverySuccesses.getOrDefault(key, new AtomicLong(0)).get());
            keyStat.put("failures", recoveryFailures.getOrDefault(key, new AtomicLong(0)).get());
            keyStats.put(key, keyStat);
        });
        stats.put("keyStats", keyStats);
        
        return stats;
    }
    
    /**
     * 일관성 체크 요약
     * @return 일관성 체크 요약 정보
     */
    public static Map<String, Object> getConsistencySummary() {
        Map<String, Object> summary = new HashMap<>();
        
        int totalStates = dataStates.size();
        long consistentCount = dataStates.values().stream()
            .filter(DataState::isConsistent)
            .count();
        long inconsistentCount = totalStates - consistentCount;
        
        summary.put("totalDataStates", totalStates);
        summary.put("consistentCount", consistentCount);
        summary.put("inconsistentCount", inconsistentCount);
        summary.put("consistencyRate", totalStates > 0 ? 
            (double) consistentCount / totalStates * 100 : 100.0);
        
        // 불일치 키 목록
        List<String> inconsistentKeys = dataStates.entrySet().stream()
            .filter(entry -> !entry.getValue().isConsistent())
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
        summary.put("inconsistentKeys", inconsistentKeys);
        
        return summary;
    }
    
    /**
     * 데이터 상태 리셋
     * @param key 데이터 키
     */
    public static void resetDataState(String key) {
        dataStates.remove(key);
        lastCheckTimes.remove(key);
        inconsistencies.remove(key);
        recoveryAttempts.remove(key);
        recoverySuccesses.remove(key);
        recoveryFailures.remove(key);
        logger.info("데이터 상태 리셋: {}", key);
    }
    
    /**
     * 모든 데이터 상태 리셋
     */
    public static void resetAllDataStates() {
        dataStates.clear();
        lastCheckTimes.clear();
        inconsistencies.clear();
        recoveryAttempts.clear();
        recoverySuccesses.clear();
        recoveryFailures.clear();
        logger.info("모든 데이터 상태 리셋 완료");
    }
}
