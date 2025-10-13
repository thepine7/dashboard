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
 * 프론트엔드-백엔드 데이터 동기화 상태 모니터링 유틸리티
 * HnT Sensor API 프로젝트 전용
 * 
 * 실시간 데이터 동기화 상태를 추적하고
 * 동기화 문제를 감지하는 모니터링 시스템
 */
@Component
public class DataSyncMonitor {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSyncMonitor.class);
    
    // 동기화 상태 추적
    private static final Map<String, SyncStatus> syncStatuses = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> syncCounts = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastSyncTimes = new ConcurrentHashMap<>();
    
    // 동기화 실패 추적
    private static final Map<String, List<SyncFailure>> syncFailures = new ConcurrentHashMap<>();
    private static final int MAX_FAILURE_RECORDS = 100;
    
    // 동기화 임계값 설정
    private static final long SYNC_TIMEOUT_MS = 10000;  // 10초
    private static final int MAX_SYNC_FAILURES = 5;     // 최대 연속 실패 횟수
    
    /**
     * 동기화 상태 열거형
     */
    public enum SyncState {
        SYNCED("동기화됨"),
        SYNCING("동기화 중"),
        OUT_OF_SYNC("동기화 안됨"),
        SYNC_ERROR("동기화 오류"),
        UNKNOWN("알 수 없음");
        
        private final String description;
        
        SyncState(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 동기화 상태 클래스
     */
    public static class SyncStatus {
        private final String key;
        private SyncState state;
        private long lastSyncTime;
        private long syncDuration;
        private int consecutiveFailures;
        private String message;
        private Map<String, Object> metadata;
        
        public SyncStatus(String key) {
            this.key = key;
            this.state = SyncState.UNKNOWN;
            this.lastSyncTime = System.currentTimeMillis();
            this.syncDuration = 0;
            this.consecutiveFailures = 0;
            this.message = "";
            this.metadata = new HashMap<>();
        }
        
        // Getters and Setters
        public String getKey() { return key; }
        public SyncState getState() { return state; }
        public void setState(SyncState state) { this.state = state; }
        public long getLastSyncTime() { return lastSyncTime; }
        public void setLastSyncTime(long lastSyncTime) { this.lastSyncTime = lastSyncTime; }
        public long getSyncDuration() { return syncDuration; }
        public void setSyncDuration(long syncDuration) { this.syncDuration = syncDuration; }
        public int getConsecutiveFailures() { return consecutiveFailures; }
        public void setConsecutiveFailures(int consecutiveFailures) { 
            this.consecutiveFailures = consecutiveFailures; 
        }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void addMetadata(String key, Object value) { this.metadata.put(key, value); }
    }
    
    /**
     * 동기화 실패 정보 클래스
     */
    public static class SyncFailure {
        private final String key;
        private final long timestamp;
        private final String reason;
        private final String details;
        private final Map<String, Object> context;
        
        public SyncFailure(String key, String reason, String details) {
            this.key = key;
            this.timestamp = System.currentTimeMillis();
            this.reason = reason;
            this.details = details;
            this.context = new HashMap<>();
        }
        
        public String getKey() { return key; }
        public long getTimestamp() { return timestamp; }
        public String getReason() { return reason; }
        public String getDetails() { return details; }
        public Map<String, Object> getContext() { return context; }
        public void addContext(String key, Object value) { this.context.put(key, value); }
    }
    
    /**
     * 동기화 시작 기록
     * @param key 동기화 키 (예: userId:sensorUuid)
     */
    public static void startSync(String key) {
        SyncStatus status = syncStatuses.computeIfAbsent(key, SyncStatus::new);
        status.setState(SyncState.SYNCING);
        status.setLastSyncTime(System.currentTimeMillis());
        status.setMessage("동기화 시작");
        
        logger.debug("동기화 시작: {}", key);
    }
    
    /**
     * 동기화 성공 기록
     * @param key 동기화 키
     * @param duration 동기화 소요 시간 (밀리초)
     */
    public static void syncSuccess(String key, long duration) {
        SyncStatus status = syncStatuses.computeIfAbsent(key, SyncStatus::new);
        status.setState(SyncState.SYNCED);
        status.setLastSyncTime(System.currentTimeMillis());
        status.setSyncDuration(duration);
        status.setConsecutiveFailures(0);
        status.setMessage("동기화 성공");
        
        // 동기화 카운트 증가
        syncCounts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        lastSyncTimes.put(key, System.currentTimeMillis());
        
        logger.debug("동기화 성공: {} (소요 시간: {}ms)", key, duration);
    }
    
    /**
     * 동기화 실패 기록
     * @param key 동기화 키
     * @param reason 실패 이유
     * @param details 실패 상세 정보
     */
    public static void syncFailure(String key, String reason, String details) {
        SyncStatus status = syncStatuses.computeIfAbsent(key, SyncStatus::new);
        status.setState(SyncState.OUT_OF_SYNC);
        status.setLastSyncTime(System.currentTimeMillis());
        status.setConsecutiveFailures(status.getConsecutiveFailures() + 1);
        status.setMessage("동기화 실패: " + reason);
        
        // 실패 정보 기록
        SyncFailure failure = new SyncFailure(key, reason, details);
        syncFailures.computeIfAbsent(key, k -> new ArrayList<>()).add(failure);
        
        // 최대 기록 개수 유지
        List<SyncFailure> failures = syncFailures.get(key);
        if (failures.size() > MAX_FAILURE_RECORDS) {
            failures.remove(0);
        }
        
        // 연속 실패가 임계값을 초과하면 에러 상태로 전환
        if (status.getConsecutiveFailures() >= MAX_SYNC_FAILURES) {
            status.setState(SyncState.SYNC_ERROR);
            logger.error("동기화 에러: {} (연속 실패 {}회)", key, status.getConsecutiveFailures());
        } else {
            logger.warn("동기화 실패: {} - {} (연속 실패 {}회)", key, reason, status.getConsecutiveFailures());
        }
    }
    
    /**
     * 동기화 상태 조회
     * @param key 동기화 키
     * @return 동기화 상태
     */
    public static SyncStatus getSyncStatus(String key) {
        return syncStatuses.get(key);
    }
    
    /**
     * 모든 동기화 상태 조회
     * @return 모든 동기화 상태
     */
    public static Map<String, SyncStatus> getAllSyncStatuses() {
        return new HashMap<>(syncStatuses);
    }
    
    /**
     * 동기화 타임아웃 체크
     * @param key 동기화 키
     * @return 타임아웃 여부
     */
    public static boolean isSyncTimeout(String key) {
        SyncStatus status = syncStatuses.get(key);
        if (status == null) {
            return false;
        }
        
        long timeSinceLastSync = System.currentTimeMillis() - status.getLastSyncTime();
        return timeSinceLastSync > SYNC_TIMEOUT_MS && status.getState() == SyncState.SYNCING;
    }
    
    /**
     * 동기화 통계 조회
     * @return 동기화 통계
     */
    public static Map<String, Object> getSyncStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 전체 통계
        long totalSyncs = syncCounts.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();
        stats.put("totalSyncs", totalSyncs);
        
        // 상태별 통계
        Map<SyncState, Long> stateStats = new HashMap<>();
        syncStatuses.values().forEach(status -> {
            stateStats.merge(status.getState(), 1L, Long::sum);
        });
        stats.put("stateStats", stateStats);
        
        // 키별 통계
        Map<String, Long> keyStats = new HashMap<>();
        syncCounts.forEach((key, count) -> keyStats.put(key, count.get()));
        stats.put("keyStats", keyStats);
        
        // 실패 통계
        int totalFailures = syncFailures.values().stream()
            .mapToInt(List::size)
            .sum();
        stats.put("totalFailures", totalFailures);
        
        // 최근 동기화 시간
        stats.put("lastSyncTimes", new HashMap<>(lastSyncTimes));
        
        return stats;
    }
    
    /**
     * 최근 동기화 실패 목록 조회
     * @param key 동기화 키
     * @param limit 조회 개수
     * @return 최근 실패 목록
     */
    public static List<SyncFailure> getRecentFailures(String key, int limit) {
        List<SyncFailure> failures = syncFailures.get(key);
        if (failures == null || failures.isEmpty()) {
            return new ArrayList<>();
        }
        
        int size = failures.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(failures.subList(fromIndex, size));
    }
    
    /**
     * 동기화 상태 리셋
     * @param key 동기화 키
     */
    public static void resetSyncStatus(String key) {
        syncStatuses.remove(key);
        syncCounts.remove(key);
        lastSyncTimes.remove(key);
        syncFailures.remove(key);
        logger.info("동기화 상태 리셋: {}", key);
    }
    
    /**
     * 모든 동기화 상태 리셋
     */
    public static void resetAllSyncStatuses() {
        syncStatuses.clear();
        syncCounts.clear();
        lastSyncTimes.clear();
        syncFailures.clear();
        logger.info("모든 동기화 상태 리셋 완료");
    }
    
    /**
     * 동기화 상태 건강 체크
     * @return 건강 상태 정보
     */
    public static Map<String, Object> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        long now = System.currentTimeMillis();
        int healthyCount = 0;
        int unhealthyCount = 0;
        List<String> unhealthyKeys = new ArrayList<>();
        
        for (Map.Entry<String, SyncStatus> entry : syncStatuses.entrySet()) {
            String key = entry.getKey();
            SyncStatus status = entry.getValue();
            
            // 건강 체크 조건
            boolean isHealthy = status.getState() == SyncState.SYNCED &&
                               status.getConsecutiveFailures() == 0 &&
                               (now - status.getLastSyncTime()) < SYNC_TIMEOUT_MS;
            
            if (isHealthy) {
                healthyCount++;
            } else {
                unhealthyCount++;
                unhealthyKeys.add(key);
            }
        }
        
        health.put("healthy", healthyCount);
        health.put("unhealthy", unhealthyCount);
        health.put("unhealthyKeys", unhealthyKeys);
        health.put("overallHealth", unhealthyCount == 0 ? "HEALTHY" : "UNHEALTHY");
        health.put("timestamp", now);
        
        return health;
    }
    
    /**
     * 동기화 문제 감지
     * @return 감지된 문제 목록
     */
    public static List<Map<String, Object>> detectSyncIssues() {
        List<Map<String, Object>> issues = new ArrayList<>();
        
        for (Map.Entry<String, SyncStatus> entry : syncStatuses.entrySet()) {
            String key = entry.getKey();
            SyncStatus status = entry.getValue();
            
            // 연속 실패 감지
            if (status.getConsecutiveFailures() >= MAX_SYNC_FAILURES) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("key", key);
                issue.put("type", "CONSECUTIVE_FAILURES");
                issue.put("severity", "HIGH");
                issue.put("message", "연속 " + status.getConsecutiveFailures() + "회 동기화 실패");
                issue.put("details", status.getMessage());
                issues.add(issue);
            }
            
            // 타임아웃 감지
            if (isSyncTimeout(key)) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("key", key);
                issue.put("type", "SYNC_TIMEOUT");
                issue.put("severity", "MEDIUM");
                issue.put("message", "동기화 타임아웃 발생");
                issue.put("details", "마지막 동기화 후 " + SYNC_TIMEOUT_MS + "ms 경과");
                issues.add(issue);
            }
            
            // 에러 상태 감지
            if (status.getState() == SyncState.SYNC_ERROR) {
                Map<String, Object> issue = new HashMap<>();
                issue.put("key", key);
                issue.put("type", "SYNC_ERROR");
                issue.put("severity", "CRITICAL");
                issue.put("message", "동기화 에러 상태");
                issue.put("details", status.getMessage());
                issues.add(issue);
            }
        }
        
        return issues;
    }
}
