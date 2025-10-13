package com.andrew.hnt.api.config;

import com.andrew.hnt.api.util.ConnectionLeakDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;
import java.util.List;

/**
 * 연결 누수 모니터링 스케줄러
 * HnT Sensor API 프로젝트 전용
 * 
 * 주기적으로 연결 누수를 스캔하고 통계를 로깅
 */
@Configuration
@EnableScheduling
public class ConnectionLeakMonitorScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionLeakMonitorScheduler.class);
    
    /**
     * 연결 누수 스캔 (10초마다)
     */
    @Scheduled(fixedDelay = 10000, initialDelay = 30000)
    public void scanForConnectionLeaks() {
        try {
            int leakCount = ConnectionLeakDetector.scanForLeaks();
            
            if (leakCount > 0) {
                logger.warn("=== 연결 누수 스캔 결과 ===");
                logger.warn("감지된 누수 연결 수: {}", leakCount);
                
                // 통계 로깅
                Map<String, Object> stats = ConnectionLeakDetector.getStatistics();
                logger.warn("총 획득: {}, 총 반환: {}, 총 누수: {}, 현재 활성: {}", 
                    stats.get("totalConnectionsAcquired"),
                    stats.get("totalConnectionsReleased"),
                    stats.get("totalLeaksDetected"),
                    stats.get("activeConnectionCount"));
                logger.warn("누수율: {}%", String.format("%.2f", stats.get("leakRate")));
            }
            
        } catch (Exception e) {
            logger.error("연결 누수 스캔 중 오류 발생", e);
        }
    }
    
    /**
     * 통계 로깅 (5분마다)
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void logConnectionStatistics() {
        try {
            Map<String, Object> stats = ConnectionLeakDetector.getStatistics();
            
            logger.info("=== 연결 풀 통계 (5분) ===");
            logger.info("총 연결 획득: {}", stats.get("totalConnectionsAcquired"));
            logger.info("총 연결 반환: {}", stats.get("totalConnectionsReleased"));
            logger.info("총 누수 감지: {}", stats.get("totalLeaksDetected"));
            logger.info("현재 활성 연결: {}", stats.get("activeConnectionCount"));
            logger.info("활성 누수 연결: {}", stats.get("activeLeakedConnections"));
            logger.info("누수율: {}%", String.format("%.2f", stats.get("leakRate")));
            
            // 누수 핫스팟 로깅
            List<Map<String, Object>> hotspots = ConnectionLeakDetector.getLeakHotspots();
            if (!hotspots.isEmpty()) {
                logger.warn("=== 누수 핫스팟 (상위 5개) ===");
                int count = Math.min(5, hotspots.size());
                for (int i = 0; i < count; i++) {
                    Map<String, Object> hotspot = hotspots.get(i);
                    logger.warn("{}. {} - 누수 횟수: {}", 
                        i + 1, hotspot.get("caller"), hotspot.get("leakCount"));
                }
            }
            
        } catch (Exception e) {
            logger.error("연결 통계 로깅 중 오류 발생", e);
        }
    }
    
    /**
     * 장시간 보유 연결 경고 (1분마다)
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void warnLongRunningConnections() {
        try {
            // 20초 이상 보유된 연결 조회
            List<ConnectionLeakDetector.ConnectionInfo> longRunning = ConnectionLeakDetector.getLongRunningConnections(20000);
            
            if (!longRunning.isEmpty()) {
                logger.warn("=== 장시간 보유 연결 경고 ===");
                logger.warn("20초 이상 보유된 연결: {}개", longRunning.size());
                
                // 상위 5개 로깅
                int count = Math.min(5, longRunning.size());
                for (int i = 0; i < count; i++) {
                    ConnectionLeakDetector.ConnectionInfo info = longRunning.get(i);
                    logger.warn("{}. {} - 보유 시간: {}ms, 스레드: {}, 호출자: {}.{}()", 
                        i + 1, 
                        info.getConnectionId(), 
                        info.getAge(),
                        info.getThreadName(),
                        info.getCallerClass(),
                        info.getCallerMethod());
                }
            }
            
        } catch (Exception e) {
            logger.error("장시간 보유 연결 경고 중 오류 발생", e);
        }
    }
}
