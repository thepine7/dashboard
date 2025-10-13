package com.andrew.hnt.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 메모리 사용량 최적화 및 모니터링 서비스
 */
@Service
public class MemoryOptimizationService {

    private static final Logger logger = LoggerFactory.getLogger(MemoryOptimizationService.class);
    
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    
    private final AtomicLong totalGcCount = new AtomicLong(0);
    private final AtomicLong totalGcTime = new AtomicLong(0);
    
    /**
     * 메모리 사용량 정보 조회
     */
    public Map<String, Object> getMemoryInfo() {
        Map<String, Object> memoryInfo = new HashMap<>();
        
        // 힙 메모리 정보
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        Map<String, Object> heap = new HashMap<>();
        heap.put("used", heapUsage.getUsed());
        heap.put("max", heapUsage.getMax());
        heap.put("committed", heapUsage.getCommitted());
        heap.put("init", heapUsage.getInit());
        heap.put("usagePercentage", (double) heapUsage.getUsed() / heapUsage.getMax() * 100);
        memoryInfo.put("heap", heap);
        
        // 비힙 메모리 정보
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        Map<String, Object> nonHeap = new HashMap<>();
        nonHeap.put("used", nonHeapUsage.getUsed());
        nonHeap.put("max", nonHeapUsage.getMax());
        nonHeap.put("committed", nonHeapUsage.getCommitted());
        nonHeap.put("init", nonHeapUsage.getInit());
        memoryInfo.put("nonHeap", nonHeap);
        
        // 가비지 컬렉션 정보
        Map<String, Object> gcInfo = new HashMap<>();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            Map<String, Object> gc = new HashMap<>();
            gc.put("name", gcBean.getName());
            gc.put("collectionCount", gcBean.getCollectionCount());
            gc.put("collectionTime", gcBean.getCollectionTime());
            gcInfo.put(gcBean.getName(), gc);
        }
        memoryInfo.put("garbageCollection", gcInfo);
        
        // 전체 GC 통계
        Map<String, Object> gcStats = new HashMap<>();
        gcStats.put("totalGcCount", totalGcCount.get());
        gcStats.put("totalGcTime", totalGcTime.get());
        memoryInfo.put("gcStats", gcStats);
        
        return memoryInfo;
    }
    
    /**
     * 메모리 사용량이 임계값을 초과했는지 확인
     */
    public boolean isMemoryUsageHigh(double thresholdPercentage) {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double usagePercentage = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
        return usagePercentage > thresholdPercentage;
    }
    
    /**
     * 가비지 컬렉션 강제 실행
     */
    public void forceGarbageCollection() {
        logger.info("가비지 컬렉션 강제 실행 시작");
        long startTime = System.currentTimeMillis();
        
        System.gc();
        
        long endTime = System.currentTimeMillis();
        logger.info("가비지 컬렉션 강제 실행 완료 - 소요시간: {}ms", endTime - startTime);
    }
    
    /**
     * 메모리 사용량 최적화 제안
     */
    public Map<String, Object> getOptimizationSuggestions() {
        Map<String, Object> suggestions = new HashMap<>();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        double usagePercentage = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
        
        if (usagePercentage > 80) {
            suggestions.put("critical", "메모리 사용량이 80%를 초과했습니다. 즉시 조치가 필요합니다.");
            suggestions.put("actions", new String[]{
                "가비지 컬렉션 강제 실행",
                "불필요한 객체 참조 해제",
                "메모리 사용량이 높은 기능 일시 중단",
                "JVM 힙 크기 증가 검토"
            });
        } else if (usagePercentage > 60) {
            suggestions.put("warning", "메모리 사용량이 60%를 초과했습니다. 모니터링이 필요합니다.");
            suggestions.put("actions", new String[]{
                "가비지 컬렉션 강제 실행",
                "메모리 사용량 모니터링 강화",
                "불필요한 객체 생성 방지"
            });
        } else {
            suggestions.put("normal", "메모리 사용량이 정상 범위입니다.");
            suggestions.put("actions", new String[]{
                "정기적인 메모리 모니터링 유지",
                "메모리 사용량 패턴 분석"
            });
        }
        
        suggestions.put("currentUsage", usagePercentage);
        suggestions.put("timestamp", System.currentTimeMillis());
        
        return suggestions;
    }
    
    /**
     * 메모리 사용량 모니터링 (5분마다 실행)
     */
    @Scheduled(fixedRate = 300000) // 5분 = 300000ms
    public void monitorMemoryUsage() {
        try {
            MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
            double usagePercentage = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
            
            logger.debug("메모리 사용량 모니터링 - 힙 사용률: {:.2f}%", usagePercentage);
            
            // 메모리 사용량이 높을 때 경고
            if (usagePercentage > 80) {
                logger.warn("메모리 사용량 경고 - 힙 사용률: {:.2f}%", usagePercentage);
                // 자동 가비지 컬렉션 실행
                forceGarbageCollection();
            }
            
            // GC 통계 업데이트
            updateGcStats();
            
        } catch (Exception e) {
            logger.error("메모리 모니터링 중 오류 발생", e);
        }
    }
    
    /**
     * GC 통계 업데이트
     */
    private void updateGcStats() {
        long totalCount = 0;
        long totalTime = 0;
        
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            totalCount += gcBean.getCollectionCount();
            totalTime += gcBean.getCollectionTime();
        }
        
        totalGcCount.set(totalCount);
        totalGcTime.set(totalTime);
    }
    
    /**
     * 메모리 사용량 히스토리 초기화
     */
    public void resetMemoryStats() {
        totalGcCount.set(0);
        totalGcTime.set(0);
        logger.info("메모리 통계 초기화 완료");
    }
}
