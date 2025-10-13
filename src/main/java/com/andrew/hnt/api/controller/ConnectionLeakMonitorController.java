package com.andrew.hnt.api.controller;

import com.andrew.hnt.api.util.ConnectionLeakDetector;
import com.andrew.hnt.api.util.ConnectionLeakDetector.ConnectionInfo;
import com.andrew.hnt.api.util.ConnectionLeakDetector.LeakEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 연결 누수 모니터링 REST API 컨트롤러
 * HnT Sensor API 프로젝트 전용
 * 
 * 연결 누수 감지 및 모니터링 API 제공
 */
@RestController
@RequestMapping("/api/connection-leak")
public class ConnectionLeakMonitorController {
    
    private static final Logger logger = LoggerFactory.getLogger(ConnectionLeakMonitorController.class);
    
    /**
     * 연결 누수 통계 조회
     * @return 통계 정보
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            Map<String, Object> stats = ConnectionLeakDetector.getStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", stats);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("연결 누수 통계 조회 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "통계 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 활성 연결 목록 조회
     * @return 활성 연결 목록
     */
    @GetMapping("/active-connections")
    public ResponseEntity<Map<String, Object>> getActiveConnections() {
        try {
            Map<String, ConnectionInfo> connections = ConnectionLeakDetector.getAllActiveConnections();
            
            // 연결 정보를 Map으로 변환
            List<Map<String, Object>> connectionList = connections.values().stream()
                .map(this::connectionInfoToMap)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", connectionList.size());
            response.put("connections", connectionList);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("활성 연결 목록 조회 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "활성 연결 목록 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 누수 이벤트 조회
     * @param caller 호출자 (클래스.메서드)
     * @param limit 조회 개수 (기본값: 20)
     * @return 누수 이벤트 목록
     */
    @GetMapping("/leak-events")
    public ResponseEntity<Map<String, Object>> getLeakEvents(
            @RequestParam(required = false) String caller,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            
            if (caller != null && !caller.isEmpty()) {
                // 특정 호출자의 누수 이벤트 조회
                List<LeakEvent> events = ConnectionLeakDetector.getLeakEvents(caller, limit);
                response.put("caller", caller);
                response.put("count", events.size());
                response.put("events", events.stream()
                    .map(this::leakEventToMap)
                    .collect(Collectors.toList()));
            } else {
                // 모든 누수 이벤트 조회
                Map<String, List<LeakEvent>> allEvents = ConnectionLeakDetector.getAllLeakEvents();
                Map<String, Object> eventsMap = new HashMap<>();
                allEvents.forEach((key, events) -> {
                    int size = events.size();
                    int fromIndex = Math.max(0, size - limit);
                    eventsMap.put(key, events.subList(fromIndex, size).stream()
                        .map(this::leakEventToMap)
                        .collect(Collectors.toList()));
                });
                response.put("events", eventsMap);
                response.put("count", allEvents.size());
            }
            
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("누수 이벤트 조회 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "누수 이벤트 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 누수 핫스팟 조회
     * @return 누수 핫스팟 목록
     */
    @GetMapping("/hotspots")
    public ResponseEntity<Map<String, Object>> getLeakHotspots() {
        try {
            List<Map<String, Object>> hotspots = ConnectionLeakDetector.getLeakHotspots();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", hotspots.size());
            response.put("hotspots", hotspots);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("누수 핫스팟 조회 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "누수 핫스팟 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 장시간 보유 연결 조회
     * @param thresholdMs 임계값 (밀리초, 기본값: 20000)
     * @return 장시간 보유 연결 목록
     */
    @GetMapping("/long-running")
    public ResponseEntity<Map<String, Object>> getLongRunningConnections(
            @RequestParam(defaultValue = "20000") long thresholdMs) {
        try {
            List<ConnectionInfo> connections = ConnectionLeakDetector.getLongRunningConnections(thresholdMs);
            
            List<Map<String, Object>> connectionList = connections.stream()
                .map(this::connectionInfoToMap)
                .collect(Collectors.toList());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("thresholdMs", thresholdMs);
            response.put("count", connectionList.size());
            response.put("connections", connectionList);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("장시간 보유 연결 조회 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "장시간 보유 연결 조회 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 연결 누수 스캔 트리거
     * @return 스캔 결과
     */
    @PostMapping("/scan")
    public ResponseEntity<Map<String, Object>> triggerLeakScan() {
        try {
            int leakCount = ConnectionLeakDetector.scanForLeaks();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("leaksDetected", leakCount);
            response.put("message", leakCount > 0 ? 
                leakCount + "개의 누수 연결 감지됨" : "누수 연결 없음");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("수동 연결 누수 스캔 완료: {}개 감지", leakCount);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("연결 누수 스캔 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "연결 누수 스캔 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 통계 리셋
     * @return 처리 결과
     */
    @DeleteMapping("/statistics")
    public ResponseEntity<Map<String, Object>> resetStatistics() {
        try {
            ConnectionLeakDetector.resetStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "통계 리셋 완료");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("연결 누수 통계 리셋 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("통계 리셋 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "통계 리셋 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 모든 추적 정보 리셋
     * @return 처리 결과
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> resetAll() {
        try {
            ConnectionLeakDetector.resetAll();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "모든 추적 정보 리셋 완료");
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("모든 연결 추적 정보 리셋 완료");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("모든 추적 정보 리셋 실패", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "모든 추적 정보 리셋 실패: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * ConnectionInfo를 Map으로 변환
     */
    private Map<String, Object> connectionInfoToMap(ConnectionInfo info) {
        Map<String, Object> map = new HashMap<>();
        map.put("connectionId", info.getConnectionId());
        map.put("acquiredTime", info.getAcquiredTime());
        map.put("age", info.getAge());
        map.put("threadName", info.getThreadName());
        map.put("callerClass", info.getCallerClass());
        map.put("callerMethod", info.getCallerMethod());
        map.put("leaked", info.isLeaked());
        if (info.isLeaked()) {
            map.put("leakedTime", info.getLeakedTime());
        }
        map.put("metadata", info.getMetadata());
        return map;
    }
    
    /**
     * LeakEvent를 Map으로 변환
     */
    private Map<String, Object> leakEventToMap(LeakEvent event) {
        Map<String, Object> map = new HashMap<>();
        map.put("connectionId", event.getConnectionId());
        map.put("detectedTime", event.getDetectedTime());
        map.put("connectionAge", event.getConnectionAge());
        map.put("threadName", event.getThreadName());
        map.put("callerClass", event.getCallerClass());
        map.put("callerMethod", event.getCallerMethod());
        map.put("details", event.getDetails());
        return map;
    }
}
