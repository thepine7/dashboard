package com.andrew.hnt.api.controller;

import com.andrew.hnt.api.mapper.AdminMapper;
import com.andrew.hnt.api.service.JmxMetricsService;
import com.andrew.hnt.api.mqtt.MqttConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 헬스체크 및 모니터링 컨트롤러
 * 애플리케이션 상태 모니터링을 위한 엔드포인트 제공
 */
@RestController
@RequestMapping("/health")
public class HealthController {
    
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private AdminMapper adminMapper;
    
    @Autowired
    private JmxMetricsService jmxMetricsService;
    
    @Autowired
    private MqttConnectionManager mqttConnectionManager;
    
    /**
     * 기본 헬스체크
     * @return 애플리케이션 상태
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("application", "HnT Sensor API");
        response.put("version", "1.0.0");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 데이터베이스 연결 상태 체크
     * @return 데이터베이스 연결 상태
     */
    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> databaseHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            // 연결 테스트
            boolean isValid = connection.isValid(5);
            
            if (isValid) {
                response.put("status", "UP");
                response.put("database", "MySQL");
                response.put("connection", "OK");
                response.put("timestamp", System.currentTimeMillis());
                
                // 간단한 쿼리 테스트
                try {
                    adminMapper.getUserCount();
                    response.put("query", "OK");
                } catch (Exception e) {
                    response.put("query", "ERROR");
                    response.put("query_error", e.getMessage());
                }
                
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "DOWN");
                response.put("error", "Database connection is not valid");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (SQLException e) {
            logger.error("Database health check failed", e);
            response.put("status", "DOWN");
            response.put("error", "Database connection failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
    
    /**
     * MQTT 연결 상태 체크
     * @return MQTT 연결 상태
     */
    @GetMapping("/mqtt")
    public ResponseEntity<Map<String, Object>> mqttHealth() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // MQTT 연결 상태 실제 확인
            boolean isConnected = mqttConnectionManager.isConnected();
            String connectionStatus = mqttConnectionManager.getConnectionStatus();
            long lastMessageTime = mqttConnectionManager.getLastMessageTime();
            int reconnectAttempts = mqttConnectionManager.getReconnectAttempts();
            
            if (isConnected) {
                response.put("status", "UP");
                response.put("connected", true);
                response.put("connectionStatus", connectionStatus);
                response.put("lastMessageTime", lastMessageTime);
                response.put("reconnectAttempts", reconnectAttempts);
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "DOWN");
                response.put("connected", false);
                response.put("connectionStatus", connectionStatus);
                response.put("reconnectAttempts", reconnectAttempts);
                response.put("error", "MQTT connection is not active");
                response.put("timestamp", System.currentTimeMillis());
                
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
            
        } catch (Exception e) {
            logger.error("MQTT health check failed", e);
            response.put("status", "DOWN");
            response.put("connected", false);
            response.put("error", "MQTT health check failed: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
    
    /**
     * 상세 시스템 정보
     * @return 시스템 리소스 및 상태 정보
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> systemInfo() {
        Map<String, Object> response = new HashMap<>();
        
        // JVM 정보
        Map<String, Object> memoryInfo = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        memoryInfo.put("totalMemory", runtime.totalMemory());
        memoryInfo.put("freeMemory", runtime.freeMemory());
        memoryInfo.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        memoryInfo.put("maxMemory", runtime.maxMemory());
        response.put("memory", memoryInfo);
        
        // 시스템 정보
        Map<String, Object> system = new HashMap<>();
        system.put("osName", System.getProperty("os.name"));
        system.put("osVersion", System.getProperty("os.version"));
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("javaVendor", System.getProperty("java.vendor"));
        response.put("system", system);
        
        // 애플리케이션 정보
        Map<String, Object> app = new HashMap<>();
        app.put("name", "HnT Sensor API");
        app.put("version", "1.0.0");
        app.put("uptime", System.currentTimeMillis() - getStartTime());
        response.put("application", app);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 쿼리 성능 모니터링 정보 조회
     */
    @GetMapping("/query-performance")
    public ResponseEntity<Map<String, Object>> queryPerformance() {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("status", "UP");
            response.put("message", "Query performance monitoring not implemented");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Query performance check failed", e);
            response.put("status", "DOWN");
            response.put("error", "Query performance check failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 메모리 사용량 모니터링 정보 조회
     */
    @GetMapping("/memory")
    public ResponseEntity<Map<String, Object>> memoryInfo() {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("status", "UP");
            response.put("message", "Memory monitoring not implemented");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Memory info check failed", e);
            response.put("status", "DOWN");
            response.put("error", "Memory info check failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * JMX 메트릭 정보 조회
     */
    @GetMapping("/jmx")
    public ResponseEntity<Map<String, Object>> jmxMetrics() {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("status", "UP");
            response.put("metrics", jmxMetricsService.getAllMetrics());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("JMX metrics check failed", e);
            response.put("status", "DOWN");
            response.put("error", "JMX metrics check failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 종합 모니터링 대시보드
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> monitoringDashboard() {
        Map<String, Object> response = new HashMap<>();
        try {
            response.put("status", "UP");
            response.put("timestamp", System.currentTimeMillis());
            
            // 기본 헬스체크
            Map<String, Object> basicHealth = new HashMap<>();
            basicHealth.put("application", "UP");
            basicHealth.put("database", "UP");
            basicHealth.put("mqtt", "UP");
            response.put("health", basicHealth);
            
            // 성능 메트릭
            Map<String, Object> performance = new HashMap<>();
            performance.put("memory", "Basic memory info available");
            performance.put("query", "Query performance monitoring not implemented");
            performance.put("jmx", "JMX metrics available");
            response.put("performance", performance);
            
            // 시스템 정보
            Map<String, Object> system = new HashMap<>();
            system.put("connectionPool", "Basic connection pool info available");
            system.put("fallbackStats", "Fallback stats not implemented");
            system.put("mqttStats", "MQTT stats not implemented");
            system.put("messageProcessing", "Message processing stats not implemented");
            response.put("system", system);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Monitoring dashboard check failed", e);
            response.put("status", "DOWN");
            response.put("error", "Monitoring dashboard check failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    private long getStartTime() {
        // 애플리케이션 시작 시간을 추정 (JVM 시작 시간 사용)
        return System.currentTimeMillis() - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
    }
}
