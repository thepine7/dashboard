package com.andrew.hnt.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 연결 풀 성능 메트릭 수집기
 * HnT Sensor API 프로젝트 전용
 * 
 * 시계열 데이터로 연결 풀 성능 메트릭을 수집하고 분석
 */
@Component
public class PoolPerformanceMetricsCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(PoolPerformanceMetricsCollector.class);
    
    // 메트릭 데이터 저장 (최근 1시간 분량, 10초 간격으로 360개)
    private static final List<MetricSnapshot> metricHistory = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HISTORY_SIZE = 360; // 1시간 (10초 간격)
    
    // 집계 통계
    private static final Map<String, AggregatedMetrics> aggregatedMetrics = new ConcurrentHashMap<>();
    
    /**
     * 메트릭 스냅샷 클래스
     */
    public static class MetricSnapshot {
        private final long timestamp;
        private final int activeConnections;
        private final int idleConnections;
        private final int totalConnections;
        private final int threadsAwaitingConnection;
        private final double utilizationRate;
        private final long responseTime;
        private final Map<String, Object> additionalMetrics;
        
        public MetricSnapshot(int activeConnections, int idleConnections, int totalConnections,
                            int threadsAwaitingConnection, double utilizationRate, long responseTime) {
            this.timestamp = System.currentTimeMillis();
            this.activeConnections = activeConnections;
            this.idleConnections = idleConnections;
            this.totalConnections = totalConnections;
            this.threadsAwaitingConnection = threadsAwaitingConnection;
            this.utilizationRate = utilizationRate;
            this.responseTime = responseTime;
            this.additionalMetrics = new HashMap<>();
        }
        
        // Getters
        public long getTimestamp() { return timestamp; }
        public int getActiveConnections() { return activeConnections; }
        public int getIdleConnections() { return idleConnections; }
        public int getTotalConnections() { return totalConnections; }
        public int getThreadsAwaitingConnection() { return threadsAwaitingConnection; }
        public double getUtilizationRate() { return utilizationRate; }
        public long getResponseTime() { return responseTime; }
        public Map<String, Object> getAdditionalMetrics() { return additionalMetrics; }
        
        public void addAdditionalMetric(String key, Object value) {
            this.additionalMetrics.put(key, value);
        }
    }
    
    /**
     * 집계 메트릭 클래스
     */
    public static class AggregatedMetrics {
        private final String period;
        private final long startTime;
        private final long endTime;
        private final int sampleCount;
        
        private final double avgActiveConnections;
        private final double avgIdleConnections;
        private final double avgTotalConnections;
        private final double avgUtilizationRate;
        private final long avgResponseTime;
        
        private final int maxActiveConnections;
        private final int maxTotalConnections;
        private final double maxUtilizationRate;
        private final long maxResponseTime;
        
        private final int minActiveConnections;
        private final int minTotalConnections;
        private final double minUtilizationRate;
        private final long minResponseTime;
        
        private final int totalWaitingThreads;
        
        public AggregatedMetrics(String period, long startTime, long endTime, int sampleCount,
                                double avgActiveConnections, double avgIdleConnections,
                                double avgTotalConnections, double avgUtilizationRate, long avgResponseTime,
                                int maxActiveConnections, int maxTotalConnections, double maxUtilizationRate,
                                long maxResponseTime, int minActiveConnections, int minTotalConnections,
                                double minUtilizationRate, long minResponseTime, int totalWaitingThreads) {
            this.period = period;
            this.startTime = startTime;
            this.endTime = endTime;
            this.sampleCount = sampleCount;
            this.avgActiveConnections = avgActiveConnections;
            this.avgIdleConnections = avgIdleConnections;
            this.avgTotalConnections = avgTotalConnections;
            this.avgUtilizationRate = avgUtilizationRate;
            this.avgResponseTime = avgResponseTime;
            this.maxActiveConnections = maxActiveConnections;
            this.maxTotalConnections = maxTotalConnections;
            this.maxUtilizationRate = maxUtilizationRate;
            this.maxResponseTime = maxResponseTime;
            this.minActiveConnections = minActiveConnections;
            this.minTotalConnections = minTotalConnections;
            this.minUtilizationRate = minUtilizationRate;
            this.minResponseTime = minResponseTime;
            this.totalWaitingThreads = totalWaitingThreads;
        }
        
        // Getters
        public String getPeriod() { return period; }
        public long getStartTime() { return startTime; }
        public long getEndTime() { return endTime; }
        public int getSampleCount() { return sampleCount; }
        public double getAvgActiveConnections() { return avgActiveConnections; }
        public double getAvgIdleConnections() { return avgIdleConnections; }
        public double getAvgTotalConnections() { return avgTotalConnections; }
        public double getAvgUtilizationRate() { return avgUtilizationRate; }
        public long getAvgResponseTime() { return avgResponseTime; }
        public int getMaxActiveConnections() { return maxActiveConnections; }
        public int getMaxTotalConnections() { return maxTotalConnections; }
        public double getMaxUtilizationRate() { return maxUtilizationRate; }
        public long getMaxResponseTime() { return maxResponseTime; }
        public int getMinActiveConnections() { return minActiveConnections; }
        public int getMinTotalConnections() { return minTotalConnections; }
        public double getMinUtilizationRate() { return minUtilizationRate; }
        public long getMinResponseTime() { return minResponseTime; }
        public int getTotalWaitingThreads() { return totalWaitingThreads; }
    }
    
    /**
     * 메트릭 수집
     * @param activeConnections 활성 연결 수
     * @param idleConnections 유휴 연결 수
     * @param totalConnections 전체 연결 수
     * @param threadsAwaitingConnection 대기 스레드 수
     * @param responseTime 응답 시간 (밀리초)
     */
    public static void collectMetric(int activeConnections, int idleConnections, int totalConnections,
                                    int threadsAwaitingConnection, long responseTime) {
        double utilizationRate = totalConnections > 0 ? 
            (double) activeConnections / totalConnections * 100 : 0.0;
        
        MetricSnapshot snapshot = new MetricSnapshot(
            activeConnections, idleConnections, totalConnections,
            threadsAwaitingConnection, utilizationRate, responseTime
        );
        
        metricHistory.add(snapshot);
        
        // 최대 크기 유지
        if (metricHistory.size() > MAX_HISTORY_SIZE) {
            metricHistory.remove(0);
        }
        
        logger.debug("메트릭 수집: 활성={}, 유휴={}, 전체={}, 대기={}, 사용률={}%, 응답시간={}ms",
            activeConnections, idleConnections, totalConnections, threadsAwaitingConnection,
            String.format("%.2f", utilizationRate), responseTime);
    }
    
    /**
     * 최근 메트릭 조회
     * @param minutes 조회할 분 수
     * @return 메트릭 스냅샷 목록
     */
    public static List<MetricSnapshot> getRecentMetrics(int minutes) {
        long cutoffTime = System.currentTimeMillis() - (minutes * 60 * 1000L);
        
        synchronized (metricHistory) {
            return metricHistory.stream()
                .filter(snapshot -> snapshot.getTimestamp() >= cutoffTime)
                .collect(Collectors.toList());
        }
    }
    
    /**
     * 전체 메트릭 이력 조회
     * @return 전체 메트릭 스냅샷 목록
     */
    public static List<MetricSnapshot> getAllMetrics() {
        synchronized (metricHistory) {
            return new ArrayList<>(metricHistory);
        }
    }
    
    /**
     * 기간별 집계 메트릭 생성
     * @param period 기간 (5m, 15m, 30m, 1h)
     * @return 집계 메트릭
     */
    public static AggregatedMetrics aggregateMetrics(String period) {
        int minutes = parsePeriod(period);
        List<MetricSnapshot> snapshots = getRecentMetrics(minutes);
        
        if (snapshots.isEmpty()) {
            return null;
        }
        
        long startTime = snapshots.get(0).getTimestamp();
        long endTime = snapshots.get(snapshots.size() - 1).getTimestamp();
        int sampleCount = snapshots.size();
        
        // 평균 계산
        double avgActive = snapshots.stream()
            .mapToInt(MetricSnapshot::getActiveConnections)
            .average().orElse(0.0);
        double avgIdle = snapshots.stream()
            .mapToInt(MetricSnapshot::getIdleConnections)
            .average().orElse(0.0);
        double avgTotal = snapshots.stream()
            .mapToInt(MetricSnapshot::getTotalConnections)
            .average().orElse(0.0);
        double avgUtilization = snapshots.stream()
            .mapToDouble(MetricSnapshot::getUtilizationRate)
            .average().orElse(0.0);
        long avgResponseTime = (long) snapshots.stream()
            .mapToLong(MetricSnapshot::getResponseTime)
            .average().orElse(0.0);
        
        // 최대값 계산
        int maxActive = snapshots.stream()
            .mapToInt(MetricSnapshot::getActiveConnections)
            .max().orElse(0);
        int maxTotal = snapshots.stream()
            .mapToInt(MetricSnapshot::getTotalConnections)
            .max().orElse(0);
        double maxUtilization = snapshots.stream()
            .mapToDouble(MetricSnapshot::getUtilizationRate)
            .max().orElse(0.0);
        long maxResponseTime = snapshots.stream()
            .mapToLong(MetricSnapshot::getResponseTime)
            .max().orElse(0);
        
        // 최소값 계산
        int minActive = snapshots.stream()
            .mapToInt(MetricSnapshot::getActiveConnections)
            .min().orElse(0);
        int minTotal = snapshots.stream()
            .mapToInt(MetricSnapshot::getTotalConnections)
            .min().orElse(0);
        double minUtilization = snapshots.stream()
            .mapToDouble(MetricSnapshot::getUtilizationRate)
            .min().orElse(0.0);
        long minResponseTime = snapshots.stream()
            .mapToLong(MetricSnapshot::getResponseTime)
            .min().orElse(0);
        
        // 대기 스레드 합계
        int totalWaitingThreads = snapshots.stream()
            .mapToInt(MetricSnapshot::getThreadsAwaitingConnection)
            .sum();
        
        AggregatedMetrics aggregated = new AggregatedMetrics(
            period, startTime, endTime, sampleCount,
            avgActive, avgIdle, avgTotal, avgUtilization, avgResponseTime,
            maxActive, maxTotal, maxUtilization, maxResponseTime,
            minActive, minTotal, minUtilization, minResponseTime,
            totalWaitingThreads
        );
        
        aggregatedMetrics.put(period, aggregated);
        return aggregated;
    }
    
    /**
     * 기간 문자열 파싱
     * @param period 기간 (5m, 15m, 30m, 1h)
     * @return 분 수
     */
    private static int parsePeriod(String period) {
        if (period.endsWith("m")) {
            return Integer.parseInt(period.substring(0, period.length() - 1));
        } else if (period.endsWith("h")) {
            return Integer.parseInt(period.substring(0, period.length() - 1)) * 60;
        }
        return 5; // 기본값: 5분
    }
    
    /**
     * 성능 분석
     * @return 성능 분석 결과
     */
    public static Map<String, Object> analyzePerformance() {
        Map<String, Object> analysis = new HashMap<>();
        
        // 최근 5분 집계
        AggregatedMetrics last5min = aggregateMetrics("5m");
        if (last5min != null) {
            analysis.put("last5min", last5min);
        }
        
        // 최근 15분 집계
        AggregatedMetrics last15min = aggregateMetrics("15m");
        if (last15min != null) {
            analysis.put("last15min", last15min);
        }
        
        // 최근 1시간 집계
        AggregatedMetrics last1hour = aggregateMetrics("1h");
        if (last1hour != null) {
            analysis.put("last1hour", last1hour);
        }
        
        // 추세 분석
        Map<String, Object> trend = analyzeTrend();
        analysis.put("trend", trend);
        
        // 이상 감지
        List<String> anomalies = detectAnomalies();
        analysis.put("anomalies", anomalies);
        
        return analysis;
    }
    
    /**
     * 추세 분석
     * @return 추세 분석 결과
     */
    private static Map<String, Object> analyzeTrend() {
        Map<String, Object> trend = new HashMap<>();
        
        List<MetricSnapshot> last5min = getRecentMetrics(5);
        List<MetricSnapshot> last15min = getRecentMetrics(15);
        
        if (last5min.size() >= 2 && last15min.size() >= 2) {
            // 사용률 추세
            double utilizationTrend = calculateTrend(last15min, MetricSnapshot::getUtilizationRate);
            trend.put("utilizationTrend", utilizationTrend > 0 ? "INCREASING" : 
                utilizationTrend < 0 ? "DECREASING" : "STABLE");
            trend.put("utilizationTrendValue", utilizationTrend);
            
            // 응답 시간 추세
            double responseTimeTrend = calculateTrend(last15min, 
                snapshot -> (double) snapshot.getResponseTime());
            trend.put("responseTimeTrend", responseTimeTrend > 0 ? "INCREASING" : 
                responseTimeTrend < 0 ? "DECREASING" : "STABLE");
            trend.put("responseTimeTrendValue", responseTimeTrend);
            
            // 활성 연결 추세
            double activeConnectionsTrend = calculateTrend(last15min, 
                snapshot -> (double) snapshot.getActiveConnections());
            trend.put("activeConnectionsTrend", activeConnectionsTrend > 0 ? "INCREASING" : 
                activeConnectionsTrend < 0 ? "DECREASING" : "STABLE");
            trend.put("activeConnectionsTrendValue", activeConnectionsTrend);
        }
        
        return trend;
    }
    
    /**
     * 추세 계산 (선형 회귀)
     * @param snapshots 메트릭 스냅샷 목록
     * @param valueExtractor 값 추출 함수
     * @return 기울기 (양수: 증가, 음수: 감소, 0: 안정)
     */
    private static double calculateTrend(List<MetricSnapshot> snapshots, 
                                        java.util.function.Function<MetricSnapshot, Double> valueExtractor) {
        if (snapshots.size() < 2) {
            return 0.0;
        }
        
        int n = snapshots.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = valueExtractor.apply(snapshots.get(i));
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        // 기울기 계산: m = (n*sumXY - sumX*sumY) / (n*sumX2 - sumX*sumX)
        double denominator = n * sumX2 - sumX * sumX;
        if (denominator == 0) {
            return 0.0;
        }
        
        return (n * sumXY - sumX * sumY) / denominator;
    }
    
    /**
     * 이상 감지
     * @return 감지된 이상 목록
     */
    private static List<String> detectAnomalies() {
        List<String> anomalies = new ArrayList<>();
        
        List<MetricSnapshot> last5min = getRecentMetrics(5);
        if (last5min.isEmpty()) {
            return anomalies;
        }
        
        // 높은 사용률
        long highUtilization = last5min.stream()
            .filter(s -> s.getUtilizationRate() > 90)
            .count();
        if (highUtilization > last5min.size() * 0.5) {
            anomalies.add("HIGH_UTILIZATION: 최근 5분간 50% 이상의 샘플에서 사용률 > 90%");
        }
        
        // 연결 대기
        long waitingThreads = last5min.stream()
            .filter(s -> s.getThreadsAwaitingConnection() > 0)
            .count();
        if (waitingThreads > 0) {
            anomalies.add("WAITING_THREADS: 연결 대기 중인 스레드 감지 (" + waitingThreads + "회)");
        }
        
        // 느린 응답
        long slowResponse = last5min.stream()
            .filter(s -> s.getResponseTime() > 1000)
            .count();
        if (slowResponse > last5min.size() * 0.3) {
            anomalies.add("SLOW_RESPONSE: 최근 5분간 30% 이상의 샘플에서 응답 시간 > 1초");
        }
        
        // 유휴 연결 부족
        long noIdleConnections = last5min.stream()
            .filter(s -> s.getIdleConnections() == 0 && s.getTotalConnections() > 0)
            .count();
        if (noIdleConnections > last5min.size() * 0.5) {
            anomalies.add("NO_IDLE_CONNECTIONS: 최근 5분간 50% 이상의 샘플에서 유휴 연결 없음");
        }
        
        return anomalies;
    }
    
    /**
     * 메트릭 이력 리셋
     */
    public static void resetMetrics() {
        metricHistory.clear();
        aggregatedMetrics.clear();
        logger.info("연결 풀 메트릭 이력 리셋 완료");
    }
    
    /**
     * 메트릭 개수 조회
     * @return 메트릭 개수
     */
    public static int getMetricsCount() {
        return metricHistory.size();
    }
}
