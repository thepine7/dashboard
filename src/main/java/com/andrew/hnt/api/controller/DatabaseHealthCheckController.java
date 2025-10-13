package com.andrew.hnt.api.controller;

import com.andrew.hnt.api.service.DatabaseHealthCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 데이터베이스 헬스 체크 REST API 컨트롤러
 * HnT Sensor API 프로젝트 전용
 * 
 * 데이터베이스 및 연결 풀 상태 모니터링 API 제공
 */
@RestController
@RequestMapping("/api/db-health")
public class DatabaseHealthCheckController {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseHealthCheckController.class);
    
    @Autowired
    private DatabaseHealthCheckService healthCheckService;
    
    /**
     * 전체 헬스 체크
     * @return 헬스 체크 결과
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = healthCheckService.performHealthCheck();
            boolean isHealthy = (boolean) health.get("healthy");
            
            HttpStatus status = isHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(health);
            
        } catch (Exception e) {
            logger.error("헬스 체크 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("healthy", false);
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "헬스 체크 실패: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 간단한 헬스 체크 (빠른 응답)
     * @return 헬스 상태
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        try {
            Map<String, Object> health = healthCheckService.quickHealthCheck();
            boolean isHealthy = (boolean) health.get("healthy");
            
            HttpStatus status = isHealthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(health);
            
        } catch (Exception e) {
            logger.error("Ping 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("healthy", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * 연결 풀 상태 조회
     * @return 연결 풀 상태
     */
    @GetMapping("/pool-status")
    public ResponseEntity<Map<String, Object>> getPoolStatus() {
        try {
            Map<String, Object> poolStatus = healthCheckService.getPoolStatus();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("poolStatus", poolStatus);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("연결 풀 상태 조회 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "연결 풀 상태 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 연결 테스트
     * @return 연결 테스트 결과
     */
    @GetMapping("/connection-test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        try {
            Map<String, Object> connectionTest = healthCheckService.testDatabaseConnection();
            boolean connected = (boolean) connectionTest.get("connected");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", connected);
            response.put("connectionTest", connectionTest);
            response.put("timestamp", System.currentTimeMillis());
            
            HttpStatus status = connected ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            logger.error("연결 테스트 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "연결 테스트 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 풀 통계 조회
     * @return 풀 통계
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> statistics = healthCheckService.getPoolStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("풀 통계 조회 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "풀 통계 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 풀 사용률 조회
     * @return 풀 사용률
     */
    @GetMapping("/utilization")
    public ResponseEntity<Map<String, Object>> getUtilization() {
        try {
            double utilization = healthCheckService.getPoolUtilization();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("utilization", utilization);
            response.put("utilizationFormatted", String.format("%.2f%%", utilization));
            response.put("timestamp", System.currentTimeMillis());
            
            // 사용률 경고
            if (utilization > 90) {
                response.put("warning", "HIGH_UTILIZATION");
                response.put("warningMessage", "연결 풀 사용률이 높습니다 (" + String.format("%.2f%%", utilization) + ")");
            } else if (utilization > 75) {
                response.put("warning", "MEDIUM_UTILIZATION");
                response.put("warningMessage", "연결 풀 사용률이 증가하고 있습니다 (" + String.format("%.2f%%", utilization) + ")");
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("풀 사용률 조회 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "풀 사용률 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 연결 가능 여부 확인
     * @return 연결 가능 여부
     */
    @GetMapping("/connectable")
    public ResponseEntity<Map<String, Object>> checkConnectable() {
        try {
            boolean connectable = healthCheckService.isConnectable();
            
            Map<String, Object> response = new HashMap<>();
            response.put("connectable", connectable);
            response.put("status", connectable ? "AVAILABLE" : "UNAVAILABLE");
            response.put("timestamp", System.currentTimeMillis());
            
            HttpStatus status = connectable ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            logger.error("연결 가능 여부 확인 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("connectable", false);
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * Kubernetes 스타일 liveness probe
     * @return liveness 상태
     */
    @GetMapping("/liveness")
    public ResponseEntity<Map<String, Object>> liveness() {
        // 애플리케이션이 살아있는지만 확인 (데이터베이스 무관)
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Application is alive");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Kubernetes 스타일 readiness probe
     * @return readiness 상태
     */
    @GetMapping("/readiness")
    public ResponseEntity<Map<String, Object>> readiness() {
        try {
            // 애플리케이션이 요청을 처리할 준비가 되었는지 확인
            boolean connectable = healthCheckService.isConnectable();
            
            Map<String, Object> response = new HashMap<>();
            response.put("ready", connectable);
            response.put("status", connectable ? "READY" : "NOT_READY");
            response.put("message", connectable ? 
                "Application is ready to serve requests" : 
                "Application is not ready - database connection unavailable");
            response.put("timestamp", System.currentTimeMillis());
            
            HttpStatus status = connectable ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
            return ResponseEntity.status(status).body(response);
            
        } catch (Exception e) {
            logger.error("Readiness 체크 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("ready", false);
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Readiness check failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
        }
    }
}
