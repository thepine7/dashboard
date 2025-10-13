package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 실시간 데이터 업데이트 간격 최적화 유틸리티 클래스
 * HnT Sensor API 프로젝트 전용
 * 
 * 실시간 데이터 업데이트 간격을 동적으로 관리하고
 * 성능 최적화를 위한 설정을 제공
 */
@Component
public class RealtimeUpdateConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RealtimeUpdateConfig.class);
    
    // 기본 업데이트 간격 설정 (밀리초)
    // 참고: MQTT 데이터는 장치에서 5초마다 전송되며 인터럽트 방식으로 즉시 처리됨
    // 이 설정은 에러 체크 간격을 위한 것이며, 실제 데이터 수신은 MQTT 콜백으로 처리됨
    private static final int DEFAULT_UPDATE_INTERVAL = 3000;  // 3초 (에러 체크 간격 - 더 빠른 에러 감지)
    private static final int MIN_UPDATE_INTERVAL = 1000;      // 1초 (최소)
    private static final int MAX_UPDATE_INTERVAL = 15000;     // 15초 (최대 - 에러 감지 임계값)
    private static final int ERROR_DETECTION_THRESHOLD = 15000; // 15초 동안 데이터 미수신 시 에러
    private static final int MQTT_DATA_INTERVAL = 5000;       // 5초 (MQTT 장치 데이터 전송 주기 - 참고용)
    
    // 업데이트 간격 통계
    private static final Map<String, AtomicLong> updateCounts = new ConcurrentHashMap<>();
    private static final Map<String, Long> lastUpdateTimes = new ConcurrentHashMap<>();
    
    // 동적 간격 조정 설정
    private static final int PERFORMANCE_THRESHOLD = 100; // 100ms 이상 지연 시 간격 조정
    private static final int ADJUSTMENT_FACTOR = 2;       // 간격 조정 배수
    
    // 업데이트 간격 설정 클래스
    public static class UpdateIntervalConfig {
        private final int interval;
        private final String reason;
        private final long timestamp;
        private final Map<String, Object> additionalInfo;
        
        public UpdateIntervalConfig(int interval, String reason) {
            this.interval = interval;
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
            this.additionalInfo = new HashMap<>();
        }
        
        public int getInterval() {
            return interval;
        }
        
        public String getReason() {
            return reason;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public Map<String, Object> getAdditionalInfo() {
            return additionalInfo;
        }
        
        public void addInfo(String key, Object value) {
            this.additionalInfo.put(key, value);
        }
    }
    
    // 업데이트 성능 메트릭 클래스
    public static class UpdatePerformanceMetrics {
        private final long totalUpdates;
        private final long averageResponseTime;
        private final long maxResponseTime;
        private final long minResponseTime;
        private final double errorRate;
        private final long lastUpdateTime;
        
        public UpdatePerformanceMetrics(long totalUpdates, long averageResponseTime, 
                                      long maxResponseTime, long minResponseTime, 
                                      double errorRate, long lastUpdateTime) {
            this.totalUpdates = totalUpdates;
            this.averageResponseTime = averageResponseTime;
            this.maxResponseTime = maxResponseTime;
            this.minResponseTime = minResponseTime;
            this.errorRate = errorRate;
            this.lastUpdateTime = lastUpdateTime;
        }
        
        public long getTotalUpdates() {
            return totalUpdates;
        }
        
        public long getAverageResponseTime() {
            return averageResponseTime;
        }
        
        public long getMaxResponseTime() {
            return maxResponseTime;
        }
        
        public long getMinResponseTime() {
            return minResponseTime;
        }
        
        public double getErrorRate() {
            return errorRate;
        }
        
        public long getLastUpdateTime() {
            return lastUpdateTime;
        }
    }
    
    /**
     * 기본 업데이트 간격 조회
     * @return 기본 업데이트 간격 (밀리초)
     */
    public static int getDefaultUpdateInterval() {
        return DEFAULT_UPDATE_INTERVAL;
    }
    
    /**
     * 최적화된 업데이트 간격 계산
     * @param context 업데이트 컨텍스트 (예: "main", "chart", "sensorSetting")
     * @param currentInterval 현재 업데이트 간격
     * @param performanceMetrics 성능 메트릭
     * @return 최적화된 업데이트 간격 설정
     */
    public static UpdateIntervalConfig calculateOptimalInterval(String context, 
                                                              int currentInterval, 
                                                              UpdatePerformanceMetrics performanceMetrics) {
        try {
            int newInterval = currentInterval;
            String reason = "현재 간격 유지";
            
            // 성능 기반 간격 조정
            if (performanceMetrics.getAverageResponseTime() > PERFORMANCE_THRESHOLD) {
                // 응답 시간이 임계값을 초과하면 간격을 늘림
                newInterval = Math.min(currentInterval * ADJUSTMENT_FACTOR, MAX_UPDATE_INTERVAL);
                reason = "응답 시간 초과로 간격 증가: " + performanceMetrics.getAverageResponseTime() + "ms";
            } else if (performanceMetrics.getAverageResponseTime() < PERFORMANCE_THRESHOLD / 2) {
                // 응답 시간이 임계값의 절반 미만이면 간격을 줄임
                newInterval = Math.max(currentInterval / ADJUSTMENT_FACTOR, MIN_UPDATE_INTERVAL);
                reason = "응답 시간 양호로 간격 감소: " + performanceMetrics.getAverageResponseTime() + "ms";
            }
            
            // 에러율 기반 간격 조정
            if (performanceMetrics.getErrorRate() > 0.1) { // 10% 이상 에러율
                newInterval = Math.min(newInterval * 2, MAX_UPDATE_INTERVAL);
                reason += " (에러율 높음: " + String.format("%.1f", performanceMetrics.getErrorRate() * 100) + "%)";
            }
            
            UpdateIntervalConfig config = new UpdateIntervalConfig(newInterval, reason);
            config.addInfo("context", context);
            config.addInfo("previousInterval", currentInterval);
            config.addInfo("averageResponseTime", performanceMetrics.getAverageResponseTime());
            config.addInfo("errorRate", performanceMetrics.getErrorRate());
            
            return config;
            
        } catch (Exception e) {
            logger.error("최적화된 업데이트 간격 계산 중 오류 발생", e);
            return new UpdateIntervalConfig(DEFAULT_UPDATE_INTERVAL, "오류 발생으로 기본값 사용");
        }
    }
    
    /**
     * 업데이트 간격 유효성 검사
     * @param interval 검사할 간격
     * @return 유효성 여부
     */
    public static boolean isValidInterval(int interval) {
        return interval >= MIN_UPDATE_INTERVAL && interval <= MAX_UPDATE_INTERVAL;
    }
    
    /**
     * 업데이트 간격 정규화
     * @param interval 정규화할 간격
     * @return 정규화된 간격
     */
    public static int normalizeInterval(int interval) {
        if (interval < MIN_UPDATE_INTERVAL) {
            return MIN_UPDATE_INTERVAL;
        } else if (interval > MAX_UPDATE_INTERVAL) {
            return MAX_UPDATE_INTERVAL;
        }
        return interval;
    }
    
    /**
     * 업데이트 카운트 증가
     * @param context 업데이트 컨텍스트
     */
    public static void incrementUpdateCount(String context) {
        updateCounts.computeIfAbsent(context, k -> new AtomicLong(0)).incrementAndGet();
        lastUpdateTimes.put(context, System.currentTimeMillis());
    }
    
    /**
     * 업데이트 통계 조회
     * @return 업데이트 통계
     */
    public static Map<String, Object> getUpdateStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 전체 통계
        long totalUpdates = updateCounts.values().stream()
            .mapToLong(AtomicLong::get)
            .sum();
        stats.put("totalUpdates", totalUpdates);
        
        // 컨텍스트별 통계
        Map<String, Long> contextStats = new HashMap<>();
        updateCounts.forEach((key, value) -> contextStats.put(key, value.get()));
        stats.put("contextStats", contextStats);
        
        // 마지막 업데이트 시간
        stats.put("lastUpdateTimes", new HashMap<>(lastUpdateTimes));
        
        // 권장 간격
        stats.put("recommendedInterval", DEFAULT_UPDATE_INTERVAL);
        stats.put("minInterval", MIN_UPDATE_INTERVAL);
        stats.put("maxInterval", MAX_UPDATE_INTERVAL);
        
        return stats;
    }
    
    /**
     * 업데이트 간격별 성능 분석
     * @param intervals 테스트할 간격 배열
     * @return 간격별 성능 분석 결과
     */
    public static Map<Integer, Map<String, Object>> analyzeIntervalPerformance(int[] intervals) {
        Map<Integer, Map<String, Object>> analysis = new HashMap<>();
        
        for (int interval : intervals) {
            Map<String, Object> intervalStats = new HashMap<>();
            
            // 간격별 이론적 업데이트 빈도 (1분당)
            double updatesPerMinute = 60000.0 / interval;
            intervalStats.put("updatesPerMinute", updatesPerMinute);
            
            // 간격별 리소스 사용량 추정
            double resourceUsage = calculateResourceUsage(interval);
            intervalStats.put("estimatedResourceUsage", resourceUsage);
            
            // 간격별 응답성 점수
            double responsivenessScore = calculateResponsivenessScore(interval);
            intervalStats.put("responsivenessScore", responsivenessScore);
            
            // 권장 여부
            boolean recommended = interval >= MIN_UPDATE_INTERVAL && interval <= MAX_UPDATE_INTERVAL;
            intervalStats.put("recommended", recommended);
            
            analysis.put(interval, intervalStats);
        }
        
        return analysis;
    }
    
    /**
     * 리소스 사용량 계산
     * @param interval 업데이트 간격
     * @return 리소스 사용량 점수 (0-100)
     */
    private static double calculateResourceUsage(int interval) {
        // 간격이 짧을수록 리소스 사용량이 높음
        double usage = (double) (MAX_UPDATE_INTERVAL - interval) / (MAX_UPDATE_INTERVAL - MIN_UPDATE_INTERVAL);
        return Math.max(0, Math.min(100, usage * 100));
    }
    
    /**
     * 응답성 점수 계산
     * @param interval 업데이트 간격
     * @return 응답성 점수 (0-100)
     */
    private static double calculateResponsivenessScore(int interval) {
        // 간격이 짧을수록 응답성이 높음
        double score = (double) (MAX_UPDATE_INTERVAL - interval) / (MAX_UPDATE_INTERVAL - MIN_UPDATE_INTERVAL);
        return Math.max(0, Math.min(100, score * 100));
    }
    
    /**
     * 업데이트 간격 리셋
     */
    public static void resetUpdateStatistics() {
        updateCounts.clear();
        lastUpdateTimes.clear();
        logger.info("업데이트 통계가 리셋되었습니다.");
    }
    
    /**
     * 동적 간격 조정이 필요한지 확인
     * @param context 업데이트 컨텍스트
     * @param performanceMetrics 성능 메트릭
     * @return 조정 필요 여부
     */
    public static boolean needsIntervalAdjustment(String context, UpdatePerformanceMetrics performanceMetrics) {
        return performanceMetrics.getAverageResponseTime() > PERFORMANCE_THRESHOLD ||
               performanceMetrics.getErrorRate() > 0.1 ||
               performanceMetrics.getTotalUpdates() > 1000; // 1000회 이상 업데이트 시
    }
}
