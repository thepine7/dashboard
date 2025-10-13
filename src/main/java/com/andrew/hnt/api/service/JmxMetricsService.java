package com.andrew.hnt.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JMX 메트릭 서비스
 * 애플리케이션 성능 및 상태 모니터링을 위한 JMX 메트릭 제공
 */
@Service
public class JmxMetricsService {

    private static final Logger logger = LoggerFactory.getLogger(JmxMetricsService.class);
    
    private final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    
    /**
     * 전체 JMX 메트릭 정보 조회
     */
    public Map<String, Object> getAllMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            metrics.put("memory", getMemoryMetrics());
            metrics.put("threads", getThreadMetrics());
            metrics.put("system", getSystemMetrics());
            metrics.put("runtime", getRuntimeMetrics());
            metrics.put("gc", getGarbageCollectionMetrics());
            metrics.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            logger.error("JMX 메트릭 조회 중 오류 발생", e);
            metrics.put("error", e.getMessage());
        }
        
        return metrics;
    }
    
    /**
     * 메모리 메트릭 조회
     */
    public Map<String, Object> getMemoryMetrics() {
        Map<String, Object> memory = new HashMap<>();
        
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        Map<String, Object> heap = new HashMap<>();
        heap.put("used", heapUsage.getUsed());
        heap.put("max", heapUsage.getMax());
        heap.put("committed", heapUsage.getCommitted());
        heap.put("init", heapUsage.getInit());
        heap.put("usagePercentage", (double) heapUsage.getUsed() / heapUsage.getMax() * 100);
        memory.put("heap", heap);
        
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        Map<String, Object> nonHeap = new HashMap<>();
        nonHeap.put("used", nonHeapUsage.getUsed());
        nonHeap.put("max", nonHeapUsage.getMax());
        nonHeap.put("committed", nonHeapUsage.getCommitted());
        nonHeap.put("init", nonHeapUsage.getInit());
        memory.put("nonHeap", nonHeap);
        
        return memory;
    }
    
    /**
     * 스레드 메트릭 조회
     */
    public Map<String, Object> getThreadMetrics() {
        Map<String, Object> threads = new HashMap<>();
        
        threads.put("currentThreadCount", threadBean.getThreadCount());
        threads.put("peakThreadCount", threadBean.getPeakThreadCount());
        threads.put("totalStartedThreadCount", threadBean.getTotalStartedThreadCount());
        threads.put("daemonThreadCount", threadBean.getDaemonThreadCount());
        
        // 데드락 감지
        long[] deadlockedThreads = threadBean.findDeadlockedThreads();
        threads.put("deadlockedThreads", deadlockedThreads != null ? deadlockedThreads.length : 0);
        
        // 모니터 데드락 감지
        long[] monitorDeadlockedThreads = threadBean.findMonitorDeadlockedThreads();
        threads.put("monitorDeadlockedThreads", monitorDeadlockedThreads != null ? monitorDeadlockedThreads.length : 0);
        
        return threads;
    }
    
    /**
     * 시스템 메트릭 조회
     */
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> system = new HashMap<>();
        
        system.put("availableProcessors", osBean.getAvailableProcessors());
        system.put("systemLoadAverage", osBean.getSystemLoadAverage());
        system.put("arch", osBean.getArch());
        system.put("name", osBean.getName());
        system.put("version", osBean.getVersion());
        
        // CPU 사용률 (Java 8 호환성 고려 - API 제한으로 인해 기본값 사용)
        system.put("processCpuLoad", 0.0);
        system.put("systemCpuLoad", 0.0);
        system.put("processCpuTime", 0L);
        
        return system;
    }
    
    /**
     * 런타임 메트릭 조회
     */
    public Map<String, Object> getRuntimeMetrics() {
        Map<String, Object> runtime = new HashMap<>();
        
        runtime.put("name", runtimeBean.getName());
        runtime.put("vmName", runtimeBean.getVmName());
        runtime.put("vmVersion", runtimeBean.getVmVersion());
        runtime.put("vmVendor", runtimeBean.getVmVendor());
        runtime.put("uptime", runtimeBean.getUptime());
        runtime.put("startTime", runtimeBean.getStartTime());
        runtime.put("inputArguments", runtimeBean.getInputArguments());
        runtime.put("systemProperties", runtimeBean.getSystemProperties());
        
        return runtime;
    }
    
    /**
     * 가비지 컬렉션 메트릭 조회
     */
    public Map<String, Object> getGarbageCollectionMetrics() {
        Map<String, Object> gc = new HashMap<>();
        
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        Map<String, Object> collectors = new HashMap<>();
        
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            Map<String, Object> collector = new HashMap<>();
            collector.put("name", gcBean.getName());
            collector.put("collectionCount", gcBean.getCollectionCount());
            collector.put("collectionTime", gcBean.getCollectionTime());
            collector.put("memoryPoolNames", gcBean.getMemoryPoolNames());
            collectors.put(gcBean.getName(), collector);
        }
        
        gc.put("collectors", collectors);
        
        // 전체 GC 통계
        long totalCollections = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
        long totalTime = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();
        
        Map<String, Object> totalStats = new HashMap<>();
        totalStats.put("totalCollections", totalCollections);
        totalStats.put("totalTime", totalTime);
        totalStats.put("averageTime", totalCollections > 0 ? (double) totalTime / totalCollections : 0.0);
        gc.put("totalStats", totalStats);
        
        return gc;
    }
    
    /**
     * 커스텀 JMX MBean 등록
     */
    public void registerCustomMBean(Object mbean, String objectName) {
        try {
            ObjectName name = new ObjectName(objectName);
            if (!mBeanServer.isRegistered(name)) {
                mBeanServer.registerMBean(mbean, name);
                logger.info("JMX MBean 등록 완료: {}", objectName);
            }
        } catch (Exception e) {
            logger.error("JMX MBean 등록 실패: {}", objectName, e);
        }
    }
    
    /**
     * 커스텀 JMX MBean 해제
     */
    public void unregisterCustomMBean(String objectName) {
        try {
            ObjectName name = new ObjectName(objectName);
            if (mBeanServer.isRegistered(name)) {
                mBeanServer.unregisterMBean(name);
                logger.info("JMX MBean 해제 완료: {}", objectName);
            }
        } catch (Exception e) {
            logger.error("JMX MBean 해제 실패: {}", objectName, e);
        }
    }
}
