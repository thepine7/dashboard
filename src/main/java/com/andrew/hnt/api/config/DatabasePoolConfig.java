package com.andrew.hnt.api.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;

/**
 * 데이터베이스 연결 풀 모니터링 설정
 * HikariCP 연결 풀 상태를 주기적으로 모니터링
 */
@Configuration
@EnableScheduling
public class DatabasePoolConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabasePoolConfig.class);
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * 5분마다 연결 풀 상태 모니터링
     */
    @Scheduled(fixedRate = 300000) // 5분
    public void monitorConnectionPool() {
        try {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();
                
                logger.info("=== 데이터베이스 연결 풀 상태 ===");
                logger.info("활성 연결 수: {}", poolBean.getActiveConnections());
                logger.info("유휴 연결 수: {}", poolBean.getIdleConnections());
                logger.info("총 연결 수: {}", poolBean.getTotalConnections());
                logger.info("대기 중인 스레드 수: {}", poolBean.getThreadsAwaitingConnection());
                
                // 연결 풀 상태 경고
                int activeConnections = poolBean.getActiveConnections();
                int totalConnections = poolBean.getTotalConnections();
                int waitingThreads = poolBean.getThreadsAwaitingConnection();
                
                // 활성 연결이 전체의 80% 이상이면 경고
                if (totalConnections > 0 && (activeConnections * 100 / totalConnections) > 80) {
                    logger.warn("연결 풀 사용률이 높습니다: {}% ({}/{})", 
                               activeConnections * 100 / totalConnections, 
                               activeConnections, totalConnections);
                }
                
                // 대기 중인 스레드가 5개 이상이면 경고
                if (waitingThreads > 5) {
                    logger.warn("연결 대기 스레드가 많습니다: {}", waitingThreads);
                }
                
                // 연결 풀 상태 평가
                String poolStatus = evaluatePoolStatus(activeConnections, totalConnections, waitingThreads);
                logger.info("연결 풀 상태: {}", poolStatus);
                
            } else {
                logger.warn("HikariCP 데이터소스가 아닙니다: {}", dataSource.getClass().getName());
            }
            
        } catch (Exception e) {
            logger.error("연결 풀 모니터링 중 오류 발생", e);
        }
    }
    
    /**
     * 연결 풀 상태 평가
     */
    private String evaluatePoolStatus(int activeConnections, int totalConnections, int waitingThreads) {
        if (totalConnections == 0) {
            return "연결 없음";
        }
        
        int usagePercent = activeConnections * 100 / totalConnections;
        
        if (waitingThreads > 10) {
            return "위험 (대기 스레드 과다)";
        } else if (usagePercent > 90) {
            return "위험 (사용률 과다)";
        } else if (usagePercent > 70) {
            return "주의 (사용률 높음)";
        } else if (waitingThreads > 5) {
            return "주의 (대기 스레드 많음)";
        } else {
            return "정상";
        }
    }
}
