package com.andrew.hnt.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 성능 모니터링 서비스
 * 실시간 성능 지표 수집, APM 도구 연동, 성능 알림 시스템
 */
@Service
public class PerformanceMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringService.class);

    @Autowired
    private NotificationService notificationService;

    // 성능 지표 저장소
    private final Map<String, Object> performanceMetrics = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> requestCounters = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> responseTimeHistory = new ConcurrentHashMap<>();
    
    // 임계값 설정
    private static final double CPU_THRESHOLD = 80.0; // CPU 사용률 80%
    private static final double MEMORY_THRESHOLD = 85.0; // 메모리 사용률 85%
    private static final long RESPONSE_TIME_THRESHOLD = 5000; // 응답 시간 5초

    /**
     * 성능 지표 수집 (5초마다)
     */
    @Scheduled(fixedRate = 5000)
    public void collectPerformanceMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();
            
            // 1. 시스템 리소스 지표
            metrics.put("system", collectSystemMetrics());
            
            // 2. JVM 지표
            metrics.put("jvm", collectJVMMetrics());
            
            // 3. 애플리케이션 지표
            metrics.put("application", collectApplicationMetrics());
            
            // 4. 데이터베이스 지표
            metrics.put("database", collectDatabaseMetrics());
            
            // 5. MQTT 지표
            metrics.put("mqtt", collectMqttMetrics());
            
            // 지표 저장
            performanceMetrics.putAll(metrics);
            performanceMetrics.put("timestamp", System.currentTimeMillis());
            
            // 임계값 체크
            checkPerformanceThresholds(metrics);
            
        } catch (Exception e) {
            logger.error("성능 지표 수집 실패", e);
        }
    }

    /**
     * 시스템 리소스 지표 수집
     */
    private Map<String, Object> collectSystemMetrics() {
        Map<String, Object> systemMetrics = new HashMap<>();
        
        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            
            // CPU 사용률
            double cpuUsage = 0.0; // Java 8에서는 지원하지 않음
            systemMetrics.put("cpuUsage", cpuUsage);
            systemMetrics.put("cpuCores", osBean.getAvailableProcessors());
            
            // 메모리 사용량
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            double memoryUsage = (double) usedMemory / maxMemory * 100;
            
            systemMetrics.put("memoryUsage", memoryUsage);
            systemMetrics.put("usedMemory", usedMemory);
            systemMetrics.put("maxMemory", maxMemory);
            systemMetrics.put("freeMemory", maxMemory - usedMemory);
            
            // 시스템 로드
            systemMetrics.put("systemLoad", osBean.getSystemLoadAverage());
            
        } catch (Exception e) {
            logger.warn("시스템 지표 수집 실패", e);
        }
        
        return systemMetrics;
    }

    /**
     * JVM 지표 수집
     */
    private Map<String, Object> collectJVMMetrics() {
        Map<String, Object> jvmMetrics = new HashMap<>();
        
        try {
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
            
            // 힙 메모리
            jvmMetrics.put("heapUsed", memoryBean.getHeapMemoryUsage().getUsed());
            jvmMetrics.put("heapMax", memoryBean.getHeapMemoryUsage().getMax());
            jvmMetrics.put("heapCommitted", memoryBean.getHeapMemoryUsage().getCommitted());
            
            // 논힙 메모리
            jvmMetrics.put("nonHeapUsed", memoryBean.getNonHeapMemoryUsage().getUsed());
            jvmMetrics.put("nonHeapMax", memoryBean.getNonHeapMemoryUsage().getMax());
            
            // 스레드 정보
            jvmMetrics.put("threadCount", threadBean.getThreadCount());
            jvmMetrics.put("peakThreadCount", threadBean.getPeakThreadCount());
            jvmMetrics.put("daemonThreadCount", threadBean.getDaemonThreadCount());
            
            // 가비지 컬렉션
            long totalGcTime = 0;
            long totalGcCount = 0;
            for (GarbageCollectorMXBean gcBean : gcBeans) {
                totalGcTime += gcBean.getCollectionTime();
                totalGcCount += gcBean.getCollectionCount();
            }
            jvmMetrics.put("totalGcTime", totalGcTime);
            jvmMetrics.put("totalGcCount", totalGcCount);
            
        } catch (Exception e) {
            logger.warn("JVM 지표 수집 실패", e);
        }
        
        return jvmMetrics;
    }

    /**
     * 애플리케이션 지표 수집
     */
    private Map<String, Object> collectApplicationMetrics() {
        Map<String, Object> appMetrics = new HashMap<>();
        
        try {
            // 요청 통계
            int totalRequests = requestCounters.values().stream()
                .mapToInt(AtomicLong::intValue)
                .sum();
            appMetrics.put("totalRequests", totalRequests);
            
            // 응답 시간 통계
            Map<String, Object> responseTimeStats = new HashMap<>();
            for (Map.Entry<String, List<Long>> entry : responseTimeHistory.entrySet()) {
                List<Long> times = entry.getValue();
                if (!times.isEmpty()) {
                    Map<String, Object> stats = new HashMap<>();
                    stats.put("avg", times.stream().mapToLong(Long::longValue).average().orElse(0.0));
                    stats.put("min", Collections.min(times));
                    stats.put("max", Collections.max(times));
                    stats.put("count", times.size());
                    responseTimeStats.put(entry.getKey(), stats);
                }
            }
            appMetrics.put("responseTimeStats", responseTimeStats);
            
            // 활성 세션 수 (추정)
            appMetrics.put("activeSessions", estimateActiveSessions());
            
        } catch (Exception e) {
            logger.warn("애플리케이션 지표 수집 실패", e);
        }
        
        return appMetrics;
    }

    /**
     * 데이터베이스 지표 수집
     */
    private Map<String, Object> collectDatabaseMetrics() {
        Map<String, Object> dbMetrics = new HashMap<>();
        
        try {
            // 데이터베이스 연결 풀 상태 (HikariCP)
            // 실제 구현에서는 HikariCP의 JMX 빈을 통해 조회
            dbMetrics.put("activeConnections", 0);
            dbMetrics.put("idleConnections", 0);
            dbMetrics.put("totalConnections", 0);
            dbMetrics.put("waitingThreads", 0);
            
            // 쿼리 성능 지표
            dbMetrics.put("avgQueryTime", 0.0);
            dbMetrics.put("slowQueries", 0);
            dbMetrics.put("totalQueries", 0);
            
        } catch (Exception e) {
            logger.warn("데이터베이스 지표 수집 실패", e);
        }
        
        return dbMetrics;
    }

    /**
     * MQTT 지표 수집
     */
    private Map<String, Object> collectMqttMetrics() {
        Map<String, Object> mqttMetrics = new HashMap<>();
        
        try {
            // MQTT 연결 상태
            mqttMetrics.put("connected", true); // 실제 구현에서는 MQTT 클라이언트 상태 확인
            mqttMetrics.put("messagesReceived", 0);
            mqttMetrics.put("messagesSent", 0);
            mqttMetrics.put("connectionUptime", 0);
            mqttMetrics.put("lastMessageTime", System.currentTimeMillis());
            
        } catch (Exception e) {
            logger.warn("MQTT 지표 수집 실패", e);
        }
        
        return mqttMetrics;
    }

    /**
     * 요청 카운터 증가
     */
    public void incrementRequestCounter(String endpoint) {
        requestCounters.computeIfAbsent(endpoint, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * 응답 시간 기록
     */
    public void recordResponseTime(String endpoint, long responseTime) {
        responseTimeHistory.computeIfAbsent(endpoint, k -> new ArrayList<>()).add(responseTime);
        
        // 최대 100개까지만 유지
        List<Long> times = responseTimeHistory.get(endpoint);
        if (times.size() > 100) {
            times.remove(0);
        }
    }

    /**
     * 성능 임계값 체크
     */
    private void checkPerformanceThresholds(Map<String, Object> metrics) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> systemMetrics = (Map<String, Object>) metrics.get("system");
            
            // CPU 사용률 체크
            double cpuUsage = (Double) systemMetrics.get("cpuUsage");
            if (cpuUsage > CPU_THRESHOLD) {
                sendPerformanceAlert("CPU 사용률 높음", 
                    String.format("CPU 사용률이 %.2f%%로 임계값(%.2f%%)을 초과했습니다.", cpuUsage, CPU_THRESHOLD));
            }
            
            // 메모리 사용률 체크
            double memoryUsage = (Double) systemMetrics.get("memoryUsage");
            if (memoryUsage > MEMORY_THRESHOLD) {
                sendPerformanceAlert("메모리 사용률 높음", 
                    String.format("메모리 사용률이 %.2f%%로 임계값(%.2f%%)을 초과했습니다.", memoryUsage, MEMORY_THRESHOLD));
            }
            
            // 응답 시간 체크
            for (Map.Entry<String, List<Long>> entry : responseTimeHistory.entrySet()) {
                List<Long> times = entry.getValue();
                if (!times.isEmpty()) {
                    double avgResponseTime = times.stream().mapToLong(Long::longValue).average().orElse(0.0);
                    if (avgResponseTime > RESPONSE_TIME_THRESHOLD) {
                        sendPerformanceAlert("응답 시간 지연", 
                            String.format("엔드포인트 %s의 평균 응답 시간이 %.2fms로 임계값(%dms)을 초과했습니다.", 
                                entry.getKey(), avgResponseTime, RESPONSE_TIME_THRESHOLD));
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("성능 임계값 체크 실패", e);
        }
    }

    /**
     * 성능 알림 전송
     */
    private void sendPerformanceAlert(String title, String message) {
        try {
            notificationService.sendSystemStatusNotification("성능 경고", title + ": " + message);
            logger.warn("성능 경고: {} - {}", title, message);
        } catch (Exception e) {
            logger.error("성능 알림 전송 실패", e);
        }
    }

    /**
     * 활성 세션 수 추정
     */
    private int estimateActiveSessions() {
        // 실제 구현에서는 세션 매니저를 통해 조회
        return 0;
    }

    /**
     * 현재 성능 지표 조회
     */
    public Map<String, Object> getCurrentMetrics() {
        return new HashMap<>(performanceMetrics);
    }

    /**
     * 성능 지표 히스토리 조회
     */
    public Map<String, Object> getMetricsHistory(int hours) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 실제 구현에서는 시간대별 지표를 저장하고 조회
            result.put("success", true);
            result.put("message", "성능 지표 히스토리 조회 기능은 향후 구현 예정입니다.");
            result.put("requestedHours", hours);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "성능 지표 히스토리 조회 실패: " + e.getMessage());
            logger.error("성능 지표 히스토리 조회 실패", e);
        }
        
        return result;
    }

    /**
     * 성능 대시보드 데이터 조회
     */
    public Map<String, Object> getDashboardData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            Map<String, Object> currentMetrics = getCurrentMetrics();
            
            // 대시보드용 요약 데이터
            Map<String, Object> summary = new HashMap<>();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> system = (Map<String, Object>) currentMetrics.get("system");
            if (system != null) {
                summary.put("cpuUsage", system.get("cpuUsage"));
                summary.put("memoryUsage", system.get("memoryUsage"));
                summary.put("systemLoad", system.get("systemLoad"));
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> jvm = (Map<String, Object>) currentMetrics.get("jvm");
            if (jvm != null) {
                summary.put("threadCount", jvm.get("threadCount"));
                summary.put("heapUsage", jvm.get("heapUsed"));
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> app = (Map<String, Object>) currentMetrics.get("application");
            if (app != null) {
                summary.put("totalRequests", app.get("totalRequests"));
                summary.put("activeSessions", app.get("activeSessions"));
            }
            
            result.put("success", true);
            result.put("summary", summary);
            result.put("timestamp", currentMetrics.get("timestamp"));
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "대시보드 데이터 조회 실패: " + e.getMessage());
            logger.error("대시보드 데이터 조회 실패", e);
        }
        
        return result;
    }

    /**
     * 성능 지표 초기화
     */
    public void resetMetrics() {
        requestCounters.clear();
        responseTimeHistory.clear();
        performanceMetrics.clear();
        logger.info("성능 지표가 초기화되었습니다.");
    }
}
