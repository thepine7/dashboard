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
 * 데이터베이스 연결 누수 감지 및 모니터링 유틸리티
 * HnT Sensor API 프로젝트 전용
 * 
 * HikariCP의 leak-detection-threshold와 함께 사용하여
 * 더 상세한 연결 누수 정보를 제공
 */
@Component
public class ConnectionLeakDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionLeakDetector.class);
    
    // 연결 추적
    private static final Map<String, ConnectionInfo> activeConnections = new ConcurrentHashMap<>();
    private static final Map<String, List<LeakEvent>> leakEvents = new ConcurrentHashMap<>();
    private static final int MAX_LEAK_EVENTS = 100;
    
    // 통계
    private static final AtomicLong totalConnectionsAcquired = new AtomicLong(0);
    private static final AtomicLong totalConnectionsReleased = new AtomicLong(0);
    private static final AtomicLong totalLeaksDetected = new AtomicLong(0);
    private static final AtomicLong activeConnectionCount = new AtomicLong(0);
    
    // 설정
    private static final long LEAK_THRESHOLD_MS = 30000;  // 30초
    private static final long SCAN_INTERVAL_MS = 10000;   // 10초
    
    /**
     * 연결 정보 클래스
     */
    public static class ConnectionInfo {
        private final String connectionId;
        private final long acquiredTime;
        private final String threadName;
        private final StackTraceElement[] stackTrace;
        private final String callerClass;
        private final String callerMethod;
        private boolean leaked;
        private long leakedTime;
        private Map<String, Object> metadata;
        
        public ConnectionInfo(String connectionId) {
            this.connectionId = connectionId;
            this.acquiredTime = System.currentTimeMillis();
            this.threadName = Thread.currentThread().getName();
            this.stackTrace = Thread.currentThread().getStackTrace();
            this.leaked = false;
            this.leakedTime = 0;
            this.metadata = new HashMap<>();
            
            // 호출자 정보 추출
            StackTraceElement caller = findCaller();
            this.callerClass = caller != null ? caller.getClassName() : "Unknown";
            this.callerMethod = caller != null ? caller.getMethodName() : "Unknown";
        }
        
        private StackTraceElement findCaller() {
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (!className.startsWith("java.") && 
                    !className.startsWith("com.zaxxer.hikari") &&
                    !className.contains("ConnectionLeakDetector")) {
                    return element;
                }
            }
            return null;
        }
        
        // Getters
        public String getConnectionId() { return connectionId; }
        public long getAcquiredTime() { return acquiredTime; }
        public String getThreadName() { return threadName; }
        public StackTraceElement[] getStackTrace() { return stackTrace; }
        public String getCallerClass() { return callerClass; }
        public String getCallerMethod() { return callerMethod; }
        public boolean isLeaked() { return leaked; }
        public void setLeaked(boolean leaked) { this.leaked = leaked; }
        public long getLeakedTime() { return leakedTime; }
        public void setLeakedTime(long leakedTime) { this.leakedTime = leakedTime; }
        public Map<String, Object> getMetadata() { return metadata; }
        public void addMetadata(String key, Object value) { this.metadata.put(key, value); }
        
        public long getAge() {
            return System.currentTimeMillis() - acquiredTime;
        }
    }
    
    /**
     * 누수 이벤트 클래스
     */
    public static class LeakEvent {
        private final String connectionId;
        private final long detectedTime;
        private final long connectionAge;
        private final String threadName;
        private final String callerClass;
        private final String callerMethod;
        private final StackTraceElement[] stackTrace;
        private final String details;
        
        public LeakEvent(ConnectionInfo connectionInfo, String details) {
            this.connectionId = connectionInfo.getConnectionId();
            this.detectedTime = System.currentTimeMillis();
            this.connectionAge = connectionInfo.getAge();
            this.threadName = connectionInfo.getThreadName();
            this.callerClass = connectionInfo.getCallerClass();
            this.callerMethod = connectionInfo.getCallerMethod();
            this.stackTrace = connectionInfo.getStackTrace();
            this.details = details;
        }
        
        // Getters
        public String getConnectionId() { return connectionId; }
        public long getDetectedTime() { return detectedTime; }
        public long getConnectionAge() { return connectionAge; }
        public String getThreadName() { return threadName; }
        public String getCallerClass() { return callerClass; }
        public String getCallerMethod() { return callerMethod; }
        public StackTraceElement[] getStackTrace() { return stackTrace; }
        public String getDetails() { return details; }
    }
    
    /**
     * 연결 획득 기록
     * @param connectionId 연결 ID
     */
    public static void onConnectionAcquired(String connectionId) {
        ConnectionInfo info = new ConnectionInfo(connectionId);
        activeConnections.put(connectionId, info);
        totalConnectionsAcquired.incrementAndGet();
        activeConnectionCount.incrementAndGet();
        
        logger.debug("연결 획득: {} (현재 활성: {})", connectionId, activeConnectionCount.get());
    }
    
    /**
     * 연결 반환 기록
     * @param connectionId 연결 ID
     */
    public static void onConnectionReleased(String connectionId) {
        ConnectionInfo info = activeConnections.remove(connectionId);
        if (info != null) {
            totalConnectionsReleased.incrementAndGet();
            activeConnectionCount.decrementAndGet();
            
            long holdTime = info.getAge();
            logger.debug("연결 반환: {} (보유 시간: {}ms, 현재 활성: {})", 
                connectionId, holdTime, activeConnectionCount.get());
            
            // 오래 보유한 연결 경고
            if (holdTime > LEAK_THRESHOLD_MS) {
                logger.warn("장시간 보유 연결 반환: {} (보유 시간: {}ms) - 호출자: {}.{}()", 
                    connectionId, holdTime, info.getCallerClass(), info.getCallerMethod());
            }
        } else {
            logger.warn("미추적 연결 반환 시도: {}", connectionId);
        }
    }
    
    /**
     * 연결 누수 스캔
     * @return 감지된 누수 연결 수
     */
    public static int scanForLeaks() {
        int leakCount = 0;
        long now = System.currentTimeMillis();
        
        for (ConnectionInfo info : activeConnections.values()) {
            long age = now - info.getAcquiredTime();
            
            if (age > LEAK_THRESHOLD_MS && !info.isLeaked()) {
                // 누수 감지
                info.setLeaked(true);
                info.setLeakedTime(now);
                leakCount++;
                totalLeaksDetected.incrementAndGet();
                
                String details = String.format(
                    "연결 누수 감지: %s (나이: %dms) - 스레드: %s, 호출자: %s.%s()",
                    info.getConnectionId(), age, info.getThreadName(), 
                    info.getCallerClass(), info.getCallerMethod()
                );
                
                logger.error(details);
                logger.error("연결 획득 스택 트레이스:", new Exception("Connection Leak Stack Trace"));
                
                // 누수 이벤트 기록
                recordLeakEvent(info, details);
            }
        }
        
        if (leakCount > 0) {
            logger.error("총 {}개의 연결 누수 감지됨 (현재 활성 연결: {})", 
                leakCount, activeConnectionCount.get());
        }
        
        return leakCount;
    }
    
    /**
     * 누수 이벤트 기록
     */
    private static void recordLeakEvent(ConnectionInfo info, String details) {
        LeakEvent event = new LeakEvent(info, details);
        String key = info.getCallerClass() + "." + info.getCallerMethod();
        
        leakEvents.computeIfAbsent(key, k -> new ArrayList<>()).add(event);
        
        // 최대 기록 개수 유지
        List<LeakEvent> events = leakEvents.get(key);
        if (events.size() > MAX_LEAK_EVENTS) {
            events.remove(0);
        }
    }
    
    /**
     * 연결 정보 조회
     * @param connectionId 연결 ID
     * @return 연결 정보
     */
    public static ConnectionInfo getConnectionInfo(String connectionId) {
        return activeConnections.get(connectionId);
    }
    
    /**
     * 모든 활성 연결 조회
     * @return 활성 연결 맵
     */
    public static Map<String, ConnectionInfo> getAllActiveConnections() {
        return new HashMap<>(activeConnections);
    }
    
    /**
     * 누수 이벤트 조회
     * @param key 호출자 키 (클래스.메서드)
     * @param limit 조회 개수
     * @return 누수 이벤트 목록
     */
    public static List<LeakEvent> getLeakEvents(String key, int limit) {
        List<LeakEvent> events = leakEvents.get(key);
        if (events == null || events.isEmpty()) {
            return new ArrayList<>();
        }
        
        int size = events.size();
        int fromIndex = Math.max(0, size - limit);
        return new ArrayList<>(events.subList(fromIndex, size));
    }
    
    /**
     * 모든 누수 이벤트 조회
     * @return 누수 이벤트 맵
     */
    public static Map<String, List<LeakEvent>> getAllLeakEvents() {
        return new HashMap<>(leakEvents);
    }
    
    /**
     * 통계 조회
     * @return 통계 정보
     */
    public static Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        long acquired = totalConnectionsAcquired.get();
        long released = totalConnectionsReleased.get();
        long leaks = totalLeaksDetected.get();
        long active = activeConnectionCount.get();
        
        stats.put("totalConnectionsAcquired", acquired);
        stats.put("totalConnectionsReleased", released);
        stats.put("totalLeaksDetected", leaks);
        stats.put("activeConnectionCount", active);
        stats.put("leakRate", acquired > 0 ? (double) leaks / acquired * 100 : 0.0);
        stats.put("activeLeakedConnections", 
            activeConnections.values().stream().filter(ConnectionInfo::isLeaked).count());
        
        return stats;
    }
    
    /**
     * 누수 핫스팟 분석
     * @return 누수가 많이 발생하는 호출자 목록
     */
    public static List<Map<String, Object>> getLeakHotspots() {
        List<Map<String, Object>> hotspots = new ArrayList<>();
        
        leakEvents.forEach((key, events) -> {
            Map<String, Object> hotspot = new HashMap<>();
            hotspot.put("caller", key);
            hotspot.put("leakCount", events.size());
            hotspot.put("lastLeakTime", events.isEmpty() ? 0 : 
                events.get(events.size() - 1).getDetectedTime());
            hotspots.add(hotspot);
        });
        
        // 누수 횟수로 정렬
        hotspots.sort((a, b) -> 
            Integer.compare((Integer) b.get("leakCount"), (Integer) a.get("leakCount")));
        
        return hotspots;
    }
    
    /**
     * 장시간 보유 연결 조회
     * @param thresholdMs 임계값 (밀리초)
     * @return 장시간 보유 연결 목록
     */
    public static List<ConnectionInfo> getLongRunningConnections(long thresholdMs) {
        List<ConnectionInfo> longRunning = new ArrayList<>();
        long now = System.currentTimeMillis();
        
        for (ConnectionInfo info : activeConnections.values()) {
            if (now - info.getAcquiredTime() > thresholdMs) {
                longRunning.add(info);
            }
        }
        
        // 보유 시간으로 정렬 (내림차순)
        longRunning.sort((a, b) -> Long.compare(b.getAge(), a.getAge()));
        
        return longRunning;
    }
    
    /**
     * 통계 리셋
     */
    public static void resetStatistics() {
        totalConnectionsAcquired.set(0);
        totalConnectionsReleased.set(0);
        totalLeaksDetected.set(0);
        activeConnectionCount.set(activeConnections.size());
        leakEvents.clear();
        logger.info("연결 누수 감지 통계 리셋 완료");
    }
    
    /**
     * 모든 추적 정보 리셋
     */
    public static void resetAll() {
        activeConnections.clear();
        leakEvents.clear();
        totalConnectionsAcquired.set(0);
        totalConnectionsReleased.set(0);
        totalLeaksDetected.set(0);
        activeConnectionCount.set(0);
        logger.info("모든 연결 추적 정보 리셋 완료");
    }
}
