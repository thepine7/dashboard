package com.andrew.hnt.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 쿼리 성능 모니터링 서비스
 */
@Service
public class QueryPerformanceService {

    private static final Logger logger = LoggerFactory.getLogger(QueryPerformanceService.class);
    
    
    // 쿼리 성능 통계 저장
    private final Map<String, QueryStats> queryStats = new ConcurrentHashMap<>();
    private final AtomicLong totalQueries = new AtomicLong(0);
    private final AtomicLong slowQueries = new AtomicLong(0);
    
    /**
     * 쿼리 실행 시간 측정 및 통계 수집
     */
    public <T> T executeWithMonitoring(String queryName, QueryOperation<T> operation) throws Exception {
        long startTime = System.currentTimeMillis();
        long endTime;
        
        try {
            T result = operation.execute();
            endTime = System.currentTimeMillis();
            
            long executionTime = endTime - startTime;
            recordQueryStats(queryName, executionTime, true);
            
            // 느린 쿼리 감지 (1초 이상)
            if (executionTime > 1000) {
                slowQueries.incrementAndGet();
                logger.warn("느린 쿼리 감지: {} - {}ms", queryName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            recordQueryStats(queryName, executionTime, false);
            logger.error("쿼리 실행 실패: {} - {}ms", queryName, executionTime, e);
            throw e;
        }
    }
    
    /**
     * 쿼리 통계 기록
     */
    private void recordQueryStats(String queryName, long executionTime, boolean success) {
        totalQueries.incrementAndGet();
        
        queryStats.computeIfAbsent(queryName, k -> new QueryStats())
            .recordExecution(executionTime, success);
    }
    
    /**
     * 쿼리 성능 통계 조회
     */
    public Map<String, Object> getQueryPerformanceStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQueries", totalQueries.get());
        stats.put("slowQueries", slowQueries.get());
        stats.put("queryDetails", queryStats);
        
        // 전체 평균 실행 시간 계산
        double avgExecutionTime = queryStats.values().stream()
            .mapToDouble(QueryStats::getAverageExecutionTime)
            .average()
            .orElse(0.0);
        stats.put("averageExecutionTime", avgExecutionTime);
        
        return stats;
    }
    
    /**
     * 느린 쿼리 목록 조회
     */
    public Map<String, Object> getSlowQueries(double thresholdMs) {
        Map<String, Object> slowQueries = new HashMap<>();
        
        queryStats.entrySet().stream()
            .filter(entry -> entry.getValue().getAverageExecutionTime() > thresholdMs)
            .forEach(entry -> {
                QueryStats stats = entry.getValue();
                Map<String, Object> queryInfo = new HashMap<>();
                queryInfo.put("averageTime", stats.getAverageExecutionTime());
                queryInfo.put("maxTime", stats.getMaxExecutionTime());
                queryInfo.put("executionCount", stats.getExecutionCount());
                queryInfo.put("successRate", stats.getSuccessRate());
                slowQueries.put(entry.getKey(), queryInfo);
            });
        
        return slowQueries;
    }
    
    /**
     * 쿼리 통계 초기화
     */
    public void resetStats() {
        queryStats.clear();
        totalQueries.set(0);
        slowQueries.set(0);
        logger.info("쿼리 성능 통계 초기화 완료");
    }
    
    /**
     * 쿼리 작업 인터페이스
     */
    @FunctionalInterface
    public interface QueryOperation<T> {
        T execute() throws Exception;
    }
    
    /**
     * 쿼리 통계 클래스
     */
    private static class QueryStats {
        private final AtomicLong executionCount = new AtomicLong(0);
        private final AtomicLong successCount = new AtomicLong(0);
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        private final AtomicLong maxExecutionTime = new AtomicLong(0);
        
        public void recordExecution(long executionTime, boolean success) {
            executionCount.incrementAndGet();
            if (success) {
                successCount.incrementAndGet();
            }
            totalExecutionTime.addAndGet(executionTime);
            maxExecutionTime.updateAndGet(current -> Math.max(current, executionTime));
        }
        
        public long getExecutionCount() {
            return executionCount.get();
        }
        
        public double getAverageExecutionTime() {
            long count = executionCount.get();
            return count > 0 ? (double) totalExecutionTime.get() / count : 0.0;
        }
        
        public long getMaxExecutionTime() {
            return maxExecutionTime.get();
        }
        
        public double getSuccessRate() {
            long count = executionCount.get();
            return count > 0 ? (double) successCount.get() / count * 100 : 0.0;
        }
    }
}
