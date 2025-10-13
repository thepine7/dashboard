package com.andrew.hnt.api.service;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * 데이터베이스 헬스 체크 서비스
 * HnT Sensor API 프로젝트 전용
 * 
 * HikariCP 연결 풀 상태를 실시간으로 모니터링
 */
@Service
public class DatabaseHealthCheckService {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthCheckService.class);
    
    @Autowired
    private DataSource dataSource;
    
    private HikariPoolMXBean hikariPoolMXBean;
    
    /**
     * HikariCP MXBean 초기화
     */
    private void initializeHikariMXBean() {
        if (hikariPoolMXBean == null && dataSource instanceof HikariDataSource) {
            try {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                String poolName = hikariDataSource.getPoolName();
                
                MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
                ObjectName objectName = new ObjectName("com.zaxxer.hikari:type=Pool (" + poolName + ")");
                hikariPoolMXBean = JMX.newMXBeanProxy(mBeanServer, objectName, HikariPoolMXBean.class);
                
                logger.info("HikariCP MXBean 초기화 완료: {}", poolName);
            } catch (Exception e) {
                logger.error("HikariCP MXBean 초기화 실패", e);
            }
        }
    }
    
    /**
     * 전체 헬스 체크
     * @return 헬스 체크 결과
     */
    public Map<String, Object> performHealthCheck() {
        initializeHikariMXBean();
        
        Map<String, Object> healthCheck = new HashMap<>();
        healthCheck.put("timestamp", System.currentTimeMillis());
        
        try {
            // 1. 데이터베이스 연결 테스트
            Map<String, Object> connectionTest = testDatabaseConnection();
            healthCheck.put("connectionTest", connectionTest);
            
            // 2. HikariCP 풀 상태
            Map<String, Object> poolStatus = getPoolStatus();
            healthCheck.put("poolStatus", poolStatus);
            
            // 3. 전체 상태 판단
            boolean isHealthy = (boolean) connectionTest.get("connected") && 
                               (boolean) poolStatus.get("healthy");
            healthCheck.put("healthy", isHealthy);
            healthCheck.put("status", isHealthy ? "UP" : "DOWN");
            
            if (isHealthy) {
                healthCheck.put("message", "데이터베이스 연결 정상");
            } else {
                healthCheck.put("message", "데이터베이스 연결 문제 감지");
            }
            
        } catch (Exception e) {
            logger.error("헬스 체크 실패", e);
            healthCheck.put("healthy", false);
            healthCheck.put("status", "ERROR");
            healthCheck.put("message", "헬스 체크 실행 중 오류 발생");
            healthCheck.put("error", e.getMessage());
        }
        
        return healthCheck;
    }
    
    /**
     * 데이터베이스 연결 테스트
     * @return 연결 테스트 결과
     */
    public Map<String, Object> testDatabaseConnection() {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();
        
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            
            boolean hasResult = resultSet.next();
            long responseTime = System.currentTimeMillis() - startTime;
            
            result.put("connected", true);
            result.put("validQuery", hasResult);
            result.put("responseTime", responseTime);
            
            if (responseTime > 1000) {
                result.put("warning", "연결 응답 시간이 느림: " + responseTime + "ms");
            }
            
            logger.debug("데이터베이스 연결 테스트 성공 (응답 시간: {}ms)", responseTime);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            result.put("connected", false);
            result.put("validQuery", false);
            result.put("responseTime", responseTime);
            result.put("error", e.getMessage());
            
            logger.error("데이터베이스 연결 테스트 실패", e);
        }
        
        return result;
    }
    
    /**
     * HikariCP 풀 상태 조회
     * @return 풀 상태 정보
     */
    public Map<String, Object> getPoolStatus() {
        initializeHikariMXBean();
        
        Map<String, Object> status = new HashMap<>();
        
        try {
            if (hikariPoolMXBean != null) {
                // 기본 상태
                int activeConnections = hikariPoolMXBean.getActiveConnections();
                int idleConnections = hikariPoolMXBean.getIdleConnections();
                int totalConnections = hikariPoolMXBean.getTotalConnections();
                int threadsAwaitingConnection = hikariPoolMXBean.getThreadsAwaitingConnection();
                
                status.put("activeConnections", activeConnections);
                status.put("idleConnections", idleConnections);
                status.put("totalConnections", totalConnections);
                status.put("threadsAwaitingConnection", threadsAwaitingConnection);
                
                // HikariDataSource 정보
                if (dataSource instanceof HikariDataSource) {
                    HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                    status.put("poolName", hikariDataSource.getPoolName());
                    status.put("minimumIdle", hikariDataSource.getMinimumIdle());
                    status.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
                    status.put("connectionTimeout", hikariDataSource.getConnectionTimeout());
                    status.put("idleTimeout", hikariDataSource.getIdleTimeout());
                    status.put("maxLifetime", hikariDataSource.getMaxLifetime());
                }
                
                // 사용률 계산
                double utilizationRate = totalConnections > 0 ? 
                    (double) activeConnections / totalConnections * 100 : 0.0;
                status.put("utilizationRate", String.format("%.2f%%", utilizationRate));
                
                // 풀 건강성 평가
                boolean isHealthy = true;
                StringBuilder healthMessage = new StringBuilder();
                
                if (threadsAwaitingConnection > 0) {
                    isHealthy = false;
                    healthMessage.append("연결 대기 중인 스레드 존재 (").append(threadsAwaitingConnection).append("개); ");
                }
                
                if (utilizationRate > 90) {
                    isHealthy = false;
                    healthMessage.append("연결 풀 사용률 높음 (").append(String.format("%.2f%%", utilizationRate)).append("); ");
                }
                
                if (idleConnections == 0 && totalConnections > 0) {
                    isHealthy = false;
                    healthMessage.append("유휴 연결 없음; ");
                }
                
                status.put("healthy", isHealthy);
                if (!isHealthy) {
                    status.put("warning", healthMessage.toString().trim());
                }
                
            } else {
                status.put("healthy", false);
                status.put("error", "HikariCP MXBean을 사용할 수 없음");
            }
            
        } catch (Exception e) {
            logger.error("풀 상태 조회 실패", e);
            status.put("healthy", false);
            status.put("error", e.getMessage());
        }
        
        return status;
    }
    
    /**
     * 풀 사용률 조회
     * @return 사용률 (0-100)
     */
    public double getPoolUtilization() {
        initializeHikariMXBean();
        
        if (hikariPoolMXBean != null) {
            int total = hikariPoolMXBean.getTotalConnections();
            int active = hikariPoolMXBean.getActiveConnections();
            return total > 0 ? (double) active / total * 100 : 0.0;
        }
        
        return 0.0;
    }
    
    /**
     * 연결 가능 여부 확인
     * @return 연결 가능 여부
     */
    public boolean isConnectable() {
        try (Connection connection = dataSource.getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (Exception e) {
            logger.error("연결 가능 여부 확인 실패", e);
            return false;
        }
    }
    
    /**
     * 간단한 헬스 체크 (빠른 응답용)
     * @return 헬스 상태
     */
    public Map<String, Object> quickHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("timestamp", System.currentTimeMillis());
        
        try {
            boolean connectable = isConnectable();
            health.put("status", connectable ? "UP" : "DOWN");
            health.put("healthy", connectable);
            
            if (connectable) {
                initializeHikariMXBean();
                if (hikariPoolMXBean != null) {
                    health.put("activeConnections", hikariPoolMXBean.getActiveConnections());
                    health.put("totalConnections", hikariPoolMXBean.getTotalConnections());
                }
            }
            
        } catch (Exception e) {
            health.put("status", "ERROR");
            health.put("healthy", false);
            health.put("error", e.getMessage());
        }
        
        return health;
    }
    
    /**
     * 풀 통계 조회
     * @return 풀 통계 정보
     */
    public Map<String, Object> getPoolStatistics() {
        initializeHikariMXBean();
        
        Map<String, Object> stats = new HashMap<>();
        
        if (hikariPoolMXBean != null) {
            stats.put("activeConnections", hikariPoolMXBean.getActiveConnections());
            stats.put("idleConnections", hikariPoolMXBean.getIdleConnections());
            stats.put("totalConnections", hikariPoolMXBean.getTotalConnections());
            stats.put("threadsAwaitingConnection", hikariPoolMXBean.getThreadsAwaitingConnection());
            
            double utilization = getPoolUtilization();
            stats.put("utilizationRate", String.format("%.2f%%", utilization));
            
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
                stats.put("minimumIdle", hikariDataSource.getMinimumIdle());
                stats.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
            }
        }
        
        return stats;
    }
}
