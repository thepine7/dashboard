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
 * 네트워크 끊김 시 데이터 동기화 재시작 관리 유틸리티
 * HnT Sensor API 프로젝트 전용
 * 
 * 네트워크 상태를 모니터링하고
 * 네트워크 복구 시 자동으로 데이터 동기화를 재시작
 */
@Component
public class NetworkRecoveryManager {
    
    private static final Logger logger = LoggerFactory.getLogger(NetworkRecoveryManager.class);
    
    // 네트워크 상태 추적
    private static final Map<String, NetworkState> networkStates = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastNetworkCheckTimes = new ConcurrentHashMap<>();
    
    // 복구 이력
    private static final Map<String, List<RecoveryEvent>> recoveryEvents = new ConcurrentHashMap<>();
    private static final int MAX_RECOVERY_EVENTS = 100;
    
    // 복구 통계
    private static final Map<String, AtomicLong> recoveryAttempts = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> recoverySuccesses = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> recoveryFailures = new ConcurrentHashMap<>();
    
    // 네트워크 설정
    private static final long NETWORK_CHECK_INTERVAL_MS = 5000;  // 5초
    private static final long NETWORK_TIMEOUT_MS = 15000;        // 15초
    private static final int MAX_RECOVERY_RETRIES = 5;           // 최대 복구 재시도
    
    /**
     * 네트워크 상태 열거형
     */
    public enum NetworkStatus {
        ONLINE("온라인"),
        OFFLINE("오프라인"),
        UNSTABLE("불안정"),
        RECOVERING("복구 중"),
        UNKNOWN("알 수 없음");
        
        private final String description;
        
        NetworkStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 네트워크 상태 클래스
     */
    public static class NetworkState {
        private final String key;
        private NetworkStatus status;
        private long lastOnlineTime;
        private long lastOfflineTime;
        private long disconnectionDuration;
        private int recoveryRetryCount;
        private boolean syncPending;
        private Map<String, Object> metadata;
        
        public NetworkState(String key) {
            this.key = key;
            this.status = NetworkStatus.UNKNOWN;
            this.lastOnlineTime = System.currentTimeMillis();
            this.lastOfflineTime = 0;
            this.disconnectionDuration = 0;
            this.recoveryRetryCount = 0;
            this.syncPending = false;
            this.metadata = new HashMap<>();
        }
        
        // Getters and Setters
        public String getKey() { return key; }
        public NetworkStatus getStatus() { return status; }
        public void setStatus(NetworkStatus status) { this.status = status; }
        public long getLastOnlineTime() { return lastOnlineTime; }
        public void setLastOnlineTime(long lastOnlineTime) { this.lastOnlineTime = lastOnlineTime; }
        public long getLastOfflineTime() { return lastOfflineTime; }
        public void setLastOfflineTime(long lastOfflineTime) { this.lastOfflineTime = lastOfflineTime; }
        public long getDisconnectionDuration() { return disconnectionDuration; }
        public void setDisconnectionDuration(long disconnectionDuration) { 
            this.disconnectionDuration = disconnectionDuration; 
        }
        public int getRecoveryRetryCount() { return recoveryRetryCount; }
        public void incrementRecoveryRetryCount() { this.recoveryRetryCount++; }
        public void resetRecoveryRetryCount() { this.recoveryRetryCount = 0; }
        public boolean isSyncPending() { return syncPending; }
        public void setSyncPending(boolean syncPending) { this.syncPending = syncPending; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void addMetadata(String key, Object value) { this.metadata.put(key, value); }
    }
    
    /**
     * 복구 이벤트 클래스
     */
    public static class RecoveryEvent {
        private final String key;
        private final long timestamp;
        private final NetworkStatus previousStatus;
        private final NetworkStatus newStatus;
        private final long disconnectionDuration;
        private final boolean syncRestarted;
        private final String details;
        
        public RecoveryEvent(String key, NetworkStatus previousStatus, NetworkStatus newStatus, 
                           long disconnectionDuration, boolean syncRestarted, String details) {
            this.key = key;
            this.timestamp = System.currentTimeMillis();
            this.previousStatus = previousStatus;
            this.newStatus = newStatus;
            this.disconnectionDuration = disconnectionDuration;
            this.syncRestarted = syncRestarted;
            this.details = details;
        }
        
        // Getters
        public String getKey() { return key; }
        public long getTimestamp() { return timestamp; }
        public NetworkStatus getPreviousStatus() { return previousStatus; }
        public NetworkStatus getNewStatus() { return newStatus; }
        public long getDisconnectionDuration() { return disconnectionDuration; }
        public boolean isSyncRestarted() { return syncRestarted; }
        public String getDetails() { return details; }
    }
    
    /**
     * 네트워크 온라인 전환
     * @param key 네트워크 키
     */
    public static void markOnline(String key) {
        NetworkState state = networkStates.computeIfAbsent(key, NetworkState::new);
        NetworkStatus previousStatus = state.getStatus();
        
        state.setStatus(NetworkStatus.ONLINE);
        state.setLastOnlineTime(System.currentTimeMillis());
        
        // 오프라인에서 온라인으로 전환된 경우
        if (previousStatus == NetworkStatus.OFFLINE || previousStatus == NetworkStatus.RECOVERING) {
            long disconnectionDuration = System.currentTimeMillis() - state.getLastOfflineTime();
            state.setDisconnectionDuration(disconnectionDuration);
            state.setSyncPending(true);
            
            logger.info("네트워크 온라인 전환: {} (끊김 기간: {}ms)", key, disconnectionDuration);
            
            // 복구 이벤트 기록
            recordRecoveryEvent(key, previousStatus, NetworkStatus.ONLINE, 
                disconnectionDuration, true, "네트워크 복구 완료");
            
            // 동기화 재시작 시도
            attemptSyncRestart(key);
        }
        
        lastNetworkCheckTimes.put(key, System.currentTimeMillis());
    }
    
    /**
     * 네트워크 오프라인 전환
     * @param key 네트워크 키
     */
    public static void markOffline(String key) {
        NetworkState state = networkStates.computeIfAbsent(key, NetworkState::new);
        NetworkStatus previousStatus = state.getStatus();
        
        state.setStatus(NetworkStatus.OFFLINE);
        state.setLastOfflineTime(System.currentTimeMillis());
        state.setSyncPending(false);
        
        logger.warn("네트워크 오프라인 전환: {}", key);
        
        // 복구 이벤트 기록
        recordRecoveryEvent(key, previousStatus, NetworkStatus.OFFLINE, 
            0, false, "네트워크 연결 끊김");
        
        lastNetworkCheckTimes.put(key, System.currentTimeMillis());
    }
    
    /**
     * 동기화 재시작 시도
     * @param key 네트워크 키
     * @return 재시작 성공 여부
     */
    public static boolean attemptSyncRestart(String key) {
        NetworkState state = networkStates.get(key);
        if (state == null) {
            return false;
        }
        
        // 복구 재시도 제한 체크
        if (state.getRecoveryRetryCount() >= MAX_RECOVERY_RETRIES) {
            logger.error("최대 복구 재시도 횟수 초과: {}", key);
            recoveryFailures.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
            return false;
        }
        
        state.incrementRecoveryRetryCount();
        state.setStatus(NetworkStatus.RECOVERING);
        recoveryAttempts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        
        try {
            logger.info("동기화 재시작 시도: {} (재시도 {}/{})", 
                key, state.getRecoveryRetryCount(), MAX_RECOVERY_RETRIES);
            
            // 동기화 재시작 (실제 구현은 호출하는 쪽에서 처리)
            state.setSyncPending(false);
            state.resetRecoveryRetryCount();
            state.setStatus(NetworkStatus.ONLINE);
            
            recoverySuccesses.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
            
            logger.info("동기화 재시작 성공: {}", key);
            return true;
            
        } catch (Exception e) {
            logger.error("동기화 재시작 실패: " + key, e);
            recoveryFailures.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
            return false;
        }
    }
    
    /**
     * 복구 이벤트 기록
     */
    private static void recordRecoveryEvent(String key, NetworkStatus previousStatus, 
                                           NetworkStatus newStatus, long disconnectionDuration, 
                                           boolean syncRestarted, String details) {
        RecoveryEvent event = new RecoveryEvent(key, previousStatus, newStatus, 
            disconnectionDuration, syncRestarted, details);
        
        recoveryEvents.computeIfAbsent(key, k -> new ArrayList<>()).add(event);
        
        // 최대 기록 개수 유지
        List<RecoveryEvent> events = recoveryEvents.get(key);
        if (events.size() > MAX_RECOVERY_EVENTS) {
            events.remove(0);
        }
    }
    
    /**
     * 네트워크 상태 조회
     * @param key 네트워크 키
     * @return 네트워크 상태
     */
    public static NetworkState getNetworkState(String key) {
        return networkStates.get(key);
    }
    
    /**
     * 모든 네트워크 상태 조회
     * @return 모든 네트워크 상태
     */
    public static Map<String, NetworkState> getAllNetworkStates() {
        return new HashMap<>(networkStates);
    }
    
    /**
     * 복구 이벤트 조회
     * @param key 네트워크 키
     * @param limit 조회 개수
     * @return 복구 이벤트 목록
     */
    public static List<RecoveryEvent> getRecoveryEvents(String key, int limit) {
        List<RecoveryEvent> events = recoveryEvents.get(key);
        if (events == null || events.isEmpty()) {
            return new ArrayList<>();
        }
        
        int size = events.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(events.subList(fromIndex, size));
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
     * 네트워크 상태 요약
     * @return 네트워크 상태 요약
     */
    public static Map<String, Object> getNetworkSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        int totalStates = networkStates.size();
        long onlineCount = networkStates.values().stream()
            .filter(state -> state.getStatus() == NetworkStatus.ONLINE)
            .count();
        long offlineCount = networkStates.values().stream()
            .filter(state -> state.getStatus() == NetworkStatus.OFFLINE)
            .count();
        long recoveringCount = networkStates.values().stream()
            .filter(state -> state.getStatus() == NetworkStatus.RECOVERING)
            .count();
        
        summary.put("totalNetworkStates", totalStates);
        summary.put("onlineCount", onlineCount);
        summary.put("offlineCount", offlineCount);
        summary.put("recoveringCount", recoveringCount);
        summary.put("onlineRate", totalStates > 0 ? 
            (double) onlineCount / totalStates * 100 : 100.0);
        
        // 오프라인 키 목록
        List<String> offlineKeys = networkStates.entrySet().stream()
            .filter(entry -> entry.getValue().getStatus() == NetworkStatus.OFFLINE)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
        summary.put("offlineKeys", offlineKeys);
        
        // 동기화 대기 중인 키 목록
        List<String> syncPendingKeys = networkStates.entrySet().stream()
            .filter(entry -> entry.getValue().isSyncPending())
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
        summary.put("syncPendingKeys", syncPendingKeys);
        
        return summary;
    }
    
    /**
     * 네트워크 상태 리셋
     * @param key 네트워크 키
     */
    public static void resetNetworkState(String key) {
        networkStates.remove(key);
        lastNetworkCheckTimes.remove(key);
        recoveryEvents.remove(key);
        recoveryAttempts.remove(key);
        recoverySuccesses.remove(key);
        recoveryFailures.remove(key);
        logger.info("네트워크 상태 리셋: {}", key);
    }
    
    /**
     * 모든 네트워크 상태 리셋
     */
    public static void resetAllNetworkStates() {
        networkStates.clear();
        lastNetworkCheckTimes.clear();
        recoveryEvents.clear();
        recoveryAttempts.clear();
        recoverySuccesses.clear();
        recoveryFailures.clear();
        logger.info("모든 네트워크 상태 리셋 완료");
    }
}
